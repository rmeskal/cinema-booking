package com.rayan.cinemaapi.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class SeatHoldServiceTests {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:latest"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private SeatHoldService seatHoldService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @AfterEach
    void cleanUp() {
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushDb();
    }

    @Test
    void holdSeat_shouldSuccessfullyHoldAvailableSeat() {
        boolean result = seatHoldService.holdSeat(1L, "A5", 10L);

        assertTrue(result);
        assertTrue(seatHoldService.isHeld(1L, "A5"));
        assertEquals(
                "10",
                redisTemplate.opsForValue().get("seat-hold:1:A5")
        );
    }

    @Test
    void holdSeat_shouldFailWhenSeatIsAlreadyHeld() {
        assertTrue(seatHoldService.holdSeat(1L, "A5", 10L));

        boolean result = seatHoldService.holdSeat(1L, "A5", 20L);

        assertFalse(result);
        assertEquals(
                "10",
                redisTemplate.opsForValue().get("seat-hold:1:A5")
        );
    }

    @Test
    void releaseSeat_shouldReleaseHoldWhenUserOwnsIt() {
        assertTrue(seatHoldService.holdSeat(1L, "A5", 10L));

        boolean result = seatHoldService.releaseSeat(1L, "A5", 10L);

        assertTrue(result);
        assertFalse(seatHoldService.isHeld(1L, "A5"));
    }

    @Test
    void releaseSeat_shouldNotReleaseHoldWhenUserDoesNotOwnIt() {
        assertTrue(seatHoldService.holdSeat(1L, "A5", 10L));

        boolean result = seatHoldService.releaseSeat(1L, "A5", 20L);

        assertFalse(result);
        assertTrue(seatHoldService.isHeld(1L, "A5"));
        assertEquals(
                "10",
                redisTemplate.opsForValue().get("seat-hold:1:A5")
        );
    }
}