package com.rayan.cinemaapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class SeatHoldService {

    // Read the owner and absolute expiry atomically, so a replacement hold's
    // expiry cannot accidentally be returned to the previous owner.
    private static final DefaultRedisScript<Long> HOLD_EXPIRY = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('PEXPIRETIME', KEYS[1])
            end
            return -2
            """, Long.class);

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
        Long expiresAt = redisTemplate.execute(
                HOLD_EXPIRY, List.of(createKey(screeningId, seatLabel)), String.valueOf(userId)
        );
        return expiresAt != null && expiresAt > 0
                ? Optional.of(Instant.ofEpochMilli(expiresAt))
                : Optional.empty();
    }

    /**
     * Atomically releases only this user's hold. Returns false when absent,
     * expired or owned by another user. Requires Redis 8.4 or newer.
     */
    public boolean releaseSeat(Long screeningId, String seatLabel, Long userId) {
        String key = createKey(screeningId, seatLabel);

        return Boolean.TRUE.equals(redisTemplate.compareAndDelete(key, String.valueOf(userId)));
    }

    private String createKey(Long screeningId, String seatLabel) {
        return "seat-hold:" + screeningId + ":" + seatLabel;
    }
}
