package com.rayan.cinemaapi.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = SeatHoldService.class,
        properties = "booking.seat-hold-duration=PT30S")
@ImportAutoConfiguration(DataRedisAutoConfiguration.class)
@Testcontainers
class SeatHoldServiceTests {

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:8.10.1"))
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
        try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
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

    @Test
    void holdSeat_shouldUseConfiguredDuration() {
        assertTrue(seatHoldService.holdSeat(1L, "A5", 10L));

        Long ttl = redisTemplate.getExpire("seat-hold:1:A5", TimeUnit.MILLISECONDS);

        assertTrue(ttl != null && ttl > 0 && ttl <= 30_000);
    }

    @Test
    void holdSeat_shouldNotRenewAnExistingHoldForTheSameUser() {
        assertTrue(seatHoldService.holdSeat(1L, "A5", 10L));
        assertTrue(redisTemplate.expire("seat-hold:1:A5", Duration.ofSeconds(5)));

        assertFalse(seatHoldService.holdSeat(1L, "A5", 10L));

        Long ttl = redisTemplate.getExpire("seat-hold:1:A5", TimeUnit.MILLISECONDS);
        assertTrue(ttl != null && ttl > 0 && ttl <= 5_000);
        assertTrue(seatHoldService.isHeldByUser(1L, "A5", 10L));
    }

    @Test
    void isHeld_shouldReturnFalseWhenNoHoldExists() {
        assertFalse(seatHoldService.isHeld(1L, "A5"));
    }

    @Test
    void isHeldByUser_shouldMatchOnlyTheCurrentOwner() {
        assertTrue(seatHoldService.holdSeat(1L, "A5", 10L));

        assertTrue(seatHoldService.isHeldByUser(1L, "A5", 10L));
        assertFalse(seatHoldService.isHeldByUser(1L, "A5", 20L));
    }

    @Test
    void isHeldByUser_shouldReturnFalseWhenNoHoldExists() {
        assertFalse(seatHoldService.isHeldByUser(1L, "A5", 10L));
    }

    @Test
    void releaseSeat_shouldReturnFalseWhenNoHoldExists() {
        assertFalse(seatHoldService.releaseSeat(1L, "A5", 10L));
    }

    @Test
    void releaseSeat_shouldReturnFalseWhenAlreadyReleased() {
        assertTrue(seatHoldService.holdSeat(1L, "A5", 10L));
        assertTrue(seatHoldService.releaseSeat(1L, "A5", 10L));

        assertFalse(seatHoldService.releaseSeat(1L, "A5", 10L));
        assertFalse(seatHoldService.isHeldByUser(1L, "A5", 10L));
    }

    @Test
    void holdSeat_shouldAllowIndependentScreeningsAndSeats() {
        assertTrue(seatHoldService.holdSeat(1L, "A5", 10L));
        assertTrue(seatHoldService.holdSeat(2L, "A5", 20L));
        assertTrue(seatHoldService.holdSeat(1L, "A6", 20L));

        assertTrue(seatHoldService.releaseSeat(1L, "A5", 10L));

        assertTrue(seatHoldService.isHeldByUser(2L, "A5", 20L));
        assertTrue(seatHoldService.isHeldByUser(1L, "A6", 20L));
    }

    @Test
    void holdSeat_shouldExpireAndAllowAnotherUserWithoutDeletingTheirHold()
            throws InterruptedException {
        SeatHoldService shortHolds = new SeatHoldService(redisTemplate, Duration.ofMillis(100));
        assertTrue(shortHolds.holdSeat(1L, "A5", 10L));

        awaitExpiry(1L, "A5");

        assertFalse(seatHoldService.isHeldByUser(1L, "A5", 10L));
        assertFalse(seatHoldService.releaseSeat(1L, "A5", 10L));
        assertTrue(seatHoldService.holdSeat(1L, "A5", 20L));

        assertFalse(seatHoldService.releaseSeat(1L, "A5", 10L));
        assertTrue(seatHoldService.isHeldByUser(1L, "A5", 20L));
    }

    @Test
    void holdSeat_shouldAllowOnlyOneConcurrentOwner() throws Exception {
        int attempts = 8;
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try (var executor = Executors.newFixedThreadPool(attempts)) {
            for (long userId = 1; userId <= attempts; userId++) {
                long holdingUserId = userId;
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent hold attempts did not start");
                    }
                    return seatHoldService.holdSeat(1L, "A5", holdingUserId);
                }));
            }

            try {
                assertTrue(ready.await(5, TimeUnit.SECONDS));
            } finally {
                start.countDown();
            }

            int successfulHolds = 0;
            for (int index = 0; index < results.size(); index++) {
                if (results.get(index).get(5, TimeUnit.SECONDS)) {
                    successfulHolds++;
                    assertTrue(seatHoldService.isHeldByUser(1L, "A5", (long) index + 1));
                }
            }
            assertEquals(1, successfulHolds);
        }
    }

    @Test
    void constructor_shouldRejectDurationsBelowOneMillisecond() {
        assertThrows(IllegalArgumentException.class,
                () -> new SeatHoldService(redisTemplate, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new SeatHoldService(redisTemplate, Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class,
                () -> new SeatHoldService(redisTemplate, Duration.ofNanos(1)));
    }

    private void awaitExpiry(Long screeningId, String seatLabel) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (seatHoldService.isHeld(screeningId, seatLabel) && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertFalse(seatHoldService.isHeld(screeningId, seatLabel), "Hold should expire in Redis");
    }
}
