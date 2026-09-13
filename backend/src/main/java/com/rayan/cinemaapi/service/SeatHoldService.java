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
        String key = "seat-hold:" + screeningId + ":" + seatLabel;

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
}