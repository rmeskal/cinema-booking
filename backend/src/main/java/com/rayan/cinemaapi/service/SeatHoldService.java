package com.rayan.cinemaapi.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SeatHoldService {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(5);

    private final RedisTemplate<String, String> redisTemplate;

    public SeatHoldService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean holdSeat(Long screeningId, String seatLabel, Long userId) {
        String key = createKey(screeningId, seatLabel);

        Boolean result = redisTemplate.opsForValue().setIfAbsent(
                key,
                String.valueOf(userId),
                HOLD_DURATION
        );

        if (result == null) {
            return false;
        }

        return result;
    }

    public boolean isHeld(Long screeningId, String seatLabel) {
        String key = createKey(screeningId, seatLabel);

        return redisTemplate.hasKey(key);
    }

    public boolean releaseSeat(Long screeningId, String seatLabel, Long userId) {
        String key = createKey(screeningId, seatLabel);

        String holdingUserId = redisTemplate.opsForValue().get(key);

        if (!String.valueOf(userId).equals(holdingUserId)) {
            return false;
        }

        Boolean result = redisTemplate.delete(key);

        if (result == null) {
            return false;
        }

        return result;
    }

    private String createKey(Long screeningId, String seatLabel) {
        return "seat-hold:" + screeningId + ":" + seatLabel;
    }
}