package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.exception.SeatHoldConflictException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SeatHoldService {

    private static final String CONFIRMING_PREFIX = "confirming:";
    private final RedisTemplate<String, String> redisTemplate;
    private final Duration holdDuration;

    public SeatHoldService(
            RedisTemplate<String, String> redisTemplate,
            @Value("${booking.seat-hold-duration:PT5M}") Duration holdDuration
    ) {
        Assert.isTrue(holdDuration != null && holdDuration.toMillis() > 0,
                "Seat hold duration must be at least one millisecond");
        this.redisTemplate = redisTemplate;
        this.holdDuration = holdDuration;
    }

    /**
     * Acquires a temporary hold. An existing hold returns false, including for
     * the same user, and its expiry is not renewed.
     */
    public boolean holdSeat(Long screeningId, String seatLabel, Long userId) {
        String key = createKey(screeningId, seatLabel);

        Boolean result = redisTemplate.opsForValue().setIfAbsent(
                key,
                String.valueOf(userId),
                holdDuration
        );

        return Boolean.TRUE.equals(result);
    }

    public boolean isHeld(Long screeningId, String seatLabel) {
        String key = createKey(screeningId, seatLabel);

        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /** Returns false for absent or expired holds, or holds owned by another user. */
    public boolean isHeldByUser(Long screeningId, String seatLabel, Long userId) {
        String key = createKey(screeningId, seatLabel);

        String holdingUserId = redisTemplate.opsForValue().get(key);

        return String.valueOf(userId).equals(holdingUserId);
    }

    /** Returns Redis's absolute expiry only for this user's active hold. */
    public Optional<Instant> getHoldExpiresAt(Long screeningId, String seatLabel, Long userId) {
        String key = createKey(screeningId, seatLabel);
        // SessionCallback keeps these commands on the same connection. EXEC reads
        // the owner, remaining TTL and Redis time together, without other clients
        // replacing the hold between reads. Redis time avoids application clock skew.
        List<Object> results = redisTemplate.execute(new SessionCallback<>() {
            @Override
            public <K, V> List<Object> execute(RedisOperations<K, V> operations) {
                operations.multi();
                redisTemplate.opsForValue().get(key);
                redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
                redisTemplate.execute((RedisCallback<Long>) connection ->
                        connection.serverCommands().time(TimeUnit.MILLISECONDS));
                return operations.exec();
            }
        });

        if (results == null || !String.valueOf(userId).equals(results.get(0))) {
            return Optional.empty();
        }
        long ttl = (Long) results.get(1);
        long redisTime = (Long) results.get(2);
        return ttl > 0 ? Optional.of(Instant.ofEpochMilli(redisTime + ttl)) : Optional.empty();
    }

    /**
     * Atomically releases only this user's hold. Returns false when absent,
     * expired or owned by another user. Requires Redis 8.4 or newer.
     */
    public boolean releaseSeat(Long screeningId, String seatLabel, Long userId) {
        String key = createKey(screeningId, seatLabel);

        return Boolean.TRUE.equals(redisTemplate.compareAndDelete(key, String.valueOf(userId)));
    }

    // The caller holds the PostgreSQL seat lock until the booking transaction ends.
    // SET IFEQ replaces only an active hold belonging to this user and removes its
    // TTL atomically. A unique value prevents delayed cleanup deleting a later hold.
    String claimForBooking(Long screeningId, String seatLabel, Long userId) {
        Instant expiresAt = getHoldExpiresAt(screeningId, seatLabel, userId)
                .orElseThrow(() -> new SeatHoldConflictException(
                        "An active seat hold belonging to this user is required."));
        String marker = CONFIRMING_PREFIX + userId + ":" + expiresAt.toEpochMilli() + ":" + UUID.randomUUID();
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().compareAndSet(
                createKey(screeningId, seatLabel), String.valueOf(userId), marker))) {
            throw new SeatHoldConflictException("Seat hold expired or changed before confirmation.");
        }
        return marker;
    }

    void completeBookingHold(Long screeningId, String seatLabel, String marker, boolean committed) {
        String key = createKey(screeningId, seatLabel);
        if (committed) {
            redisTemplate.compareAndDelete(key, marker);
        } else {
            String[] parts = marker.split(":");
            // PXAT restores the original deadline using Redis's clock. If it has
            // already passed, Redis immediately expires the restored hold.
            redisTemplate.opsForValue().set(key, parts[1], spec -> spec.ifEquals().value(marker)
                    .expiration(Expiration.unixTimestamp(Long.parseLong(parts[2]), TimeUnit.MILLISECONDS)));
        }
    }

    void verifyBookingHold(Long screeningId, String seatLabel, String marker) {
        if (!marker.equals(redisTemplate.opsForValue().get(createKey(screeningId, seatLabel)))) {
            throw new SeatHoldConflictException("Seat hold changed during booking confirmation.");
        }
    }

    // Only call while holding the SQL seat lock. Acquiring that lock proves the
    // previous SQL transaction has finished (or crashed). PostgreSQL decides recovery,
    // including when a Redis write succeeded but its response was lost.
    void recoverBookingHold(Long screeningId, String seatLabel, boolean booked) {
        String marker = redisTemplate.opsForValue().get(createKey(screeningId, seatLabel));
        if (marker != null && marker.startsWith(CONFIRMING_PREFIX)) {
            completeBookingHold(screeningId, seatLabel, marker, booked);
        }
    }

    private String createKey(Long screeningId, String seatLabel) {
        return "seat-hold:" + screeningId + ":" + seatLabel;
    }
}
