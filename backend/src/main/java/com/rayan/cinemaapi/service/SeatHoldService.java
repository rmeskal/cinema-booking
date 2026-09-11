package com.rayan.cinemaapi.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeatHoldService {

    private final RedisTemplate<String, String> redisTemplate;

    public SeatHoldService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}