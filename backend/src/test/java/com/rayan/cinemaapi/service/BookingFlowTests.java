package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.TestDataFactory;
import com.rayan.cinemaapi.entity.*;
import com.rayan.cinemaapi.repository.*;
import jakarta.persistence.EntityManager;
import io.lettuce.core.RedisCommandTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Deliberately no @Transactional: requests must exercise real SQL commits and
// transaction-completion callbacks. Fixture transactions commit before requests.
@SpringBootTest(properties = "booking.seat-hold-duration=PT30S")
@AutoConfigureMockMvc
@Testcontainers
class BookingFlowTests {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.10.1"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private BookingService bookingService;
    @Autowired private SeatService seatService;
    @MockitoSpyBean private SeatHoldService holds;
    @MockitoSpyBean private BookingRepository bookings;
    @Autowired private MovieRepository movies;
    @Autowired private RoomTypeRepository roomTypes;
    @Autowired private RoomRepository rooms;
    @Autowired private ScreeningRepository screenings;
    @Autowired private SeatRepository seats;
    @Autowired private UserRepository users;
    @Autowired private RedisTemplate<String, String> redisTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private EntityManager entityManager;

    private TransactionTemplate transactions;
    private Long screeningId;
    private Long roomId;
    private Long userId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(status -> {
            var movie = movies.save(TestDataFactory.createMovie());
            var type = roomTypes.save(TestDataFactory.createRoomType());
            var room = rooms.save(TestDataFactory.createRoom(type));
            roomId = room.getId();
            screeningId = screenings.save(TestDataFactory.createScreening(movie, room)).getId();
            entityManager.persist(TestDataFactory.createSeat(room, "A1"));
            userId = users.save(TestDataFactory.createUser()).getId();
            var other = TestDataFactory.createUser();
            other.setEmail("other@example.com");
            otherUserId = users.save(other).getId();
        });
    }

    @AfterEach
    void cleanUp() {
        reset(holds, bookings);
        transactions.executeWithoutResult(status -> {
            bookings.deleteAll();
            screenings.deleteAll();
            seats.deleteAll();
            users.deleteAll();
            rooms.deleteAll();
            roomTypes.deleteAll();
            movies.deleteAll();
        });
        try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    @Test
    void availableToHeldToBooked_commitsThenCleansHold() throws Exception {
        assertAvailability("AVAILABLE");
        acquire(userId).andExpect(status().isCreated());
        assertAvailability("HELD");

        book(userId).andExpect(status().isCreated()).andExpect(jsonPath("$.userId").value(userId));

        assertEquals(1, bookings.count());
        assertFalse(holds.isHeld(screeningId, "A1"));
        assertAvailability("BOOKED");
        acquire(otherUserId).andExpect(status().isConflict());
    }

    @Test
    void missingHold_isConflict() throws Exception {
        book(userId).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        assertEquals(0, bookings.count());
    }

    @Test
    void wrongOwner_isConflictAndPreservesHold() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        Instant expiry = holds.getHoldExpiresAt(screeningId, "A1", userId).orElseThrow();
        book(otherUserId).andExpect(status().isConflict());
        assertEquals(0, bookings.count());
        assertEquals(expiry, holds.getHoldExpiresAt(screeningId, "A1", userId).orElseThrow());
    }

    @Test
    void expiredHold_isConflict() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        redisTemplate.expire(key(), Duration.ofMillis(50));
        awaitExpiry();
        book(userId).andExpect(status().isConflict());
        assertEquals(0, bookings.count());
        assertAvailability("AVAILABLE");
    }

    @Test
    void claimedHold_cannotExpireOrBeReleasedBeforeCommit() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        redisTemplate.expire(key(), Duration.ofSeconds(1));
        transactions.executeWithoutResult(status -> {
            bookingService.createBooking(request(userId));
            assertEquals(-1L, redisTemplate.getExpire(key(), TimeUnit.MILLISECONDS));
            assertFalse(holds.releaseSeat(screeningId, "A1", userId));
            assertFalse(holds.holdSeat(screeningId, "A1", otherUserId));
            try {
                Thread.sleep(1100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            assertTrue(holds.isHeld(screeningId, "A1"));
        });
        assertEquals(1, bookings.count());
        assertFalse(holds.isHeld(screeningId, "A1"));
    }

    @Test
    void rollback_restoresOriginalHoldWithoutRenewingIt() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        Instant expiry = holds.getHoldExpiresAt(screeningId, "A1", userId).orElseThrow();
        transactions.executeWithoutResult(status -> {
            bookingService.createBooking(request(userId));
            assertTrue(redisTemplate.opsForValue().get(key()).startsWith("confirming:"));
            status.setRollbackOnly();
        });
        assertEquals(0, bookings.count());
        assertEquals(expiry, holds.getHoldExpiresAt(screeningId, "A1", userId).orElseThrow());
    }

    @Test
    void databaseFailureAfterInsert_rollsBackAndRestoresHold() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        doAnswer(invocation -> {
            entityManager.persist(invocation.getArgument(0, Booking.class));
            entityManager.flush();
            throw new DataIntegrityViolationException("Simulated failure after SQL insert");
        }).when(bookings).saveAndFlush(any(Booking.class));

        book(userId).andExpect(status().isConflict());
        assertEquals(0, bookings.count());
        assertTrue(holds.isHeldByUser(screeningId, "A1", userId));
    }

    @Test
    void databaseUniqueConstraint_stillPreventsDoubleBooking() throws Exception {
        transactions.executeWithoutResult(status -> bookings.saveAndFlush(request(otherUserId)));
        assertTrue(holds.holdSeat(screeningId, "A1", userId));
        // Bypass the friendly precheck to exercise the actual PostgreSQL constraint.
        doReturn(false).when(bookings).existsByScreeningAndSeat(screeningId, "A1", roomId);

        book(userId).andExpect(status().isConflict());
        assertEquals(1, bookings.count());
        assertTrue(holds.isHeldByUser(screeningId, "A1", userId));
    }

    @Test
    void failureAtCommit_rollsBackAndRestoresHold() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        assertThrows(IllegalStateException.class, () -> transactions.executeWithoutResult(status -> {
            bookingService.createBooking(request(userId));
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void beforeCommit(boolean readOnly) {
                    throw new IllegalStateException("Simulated commit failure");
                }
            });
        }));
        assertEquals(0, bookings.count());
        assertTrue(holds.isHeldByUser(screeningId, "A1", userId));
    }

    @Test
    void concurrentBookings_onlyOwnerSucceedsOnce() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(3)) {
            List<Future<Integer>> results = List.of(userId, userId, otherUserId).stream()
                    .map(id -> executor.submit(() -> {
                        assertTrue(start.await(5, TimeUnit.SECONDS));
                        return book(id).andReturn().getResponse().getStatus();
                    })).toList();
            start.countDown();
            int successes = 0;
            for (Future<Integer> result : results) {
                int response = result.get(10, TimeUnit.SECONDS);
                assertTrue(response == 201 || response == 409);
                if (response == 201) successes++;
            }
            assertEquals(1, successes);
        }
        assertEquals(1, bookings.count());
        assertEquals(userId, bookings.findAll().getFirst().getUser().getId());
        assertFalse(holds.isHeld(screeningId, "A1"));
    }

    @Test
    void releaseRequest_waitsForConfirmationTransaction() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        CountDownLatch ready = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            Future<?>[] release = new Future<?>[1];
            transactions.executeWithoutResult(status -> {
                bookingService.createBooking(request(userId));
                release[0] = executor.submit(() -> {
                    ready.countDown();
                    seatService.releaseSeatHold(screeningId, "A1", userId);
                });
                try {
                    assertTrue(ready.await(5, TimeUnit.SECONDS));
                    assertThrows(TimeoutException.class, () -> release[0].get(150, TimeUnit.MILLISECONDS));
                } catch (InterruptedException exception) {
                    throw new IllegalStateException(exception);
                }
            });
            release[0].get(10, TimeUnit.SECONDS);
        }
        assertEquals(1, bookings.count());
        assertFalse(holds.isHeld(screeningId, "A1"));
    }

    @Test
    void redisFailureBeforeClaim_returns503WithoutBooking() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        doThrow(redisFailure()).when(holds).getHoldExpiresAt(screeningId, "A1", userId);
        book(userId).andExpect(status().isServiceUnavailable());
        assertEquals(0, bookings.count());
        assertTrue(holds.isHeldByUser(screeningId, "A1", userId));
    }

    @Test
    void redisTimeoutBeforeClaim_returns503WithoutBooking() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        doThrow(new QueryTimeoutException("Redis command timed out",
                new RedisCommandTimeoutException("Simulated Redis timeout")))
                .when(holds).getHoldExpiresAt(screeningId, "A1", userId);

        book(userId).andExpect(status().isServiceUnavailable());
        assertEquals(0, bookings.count());
        assertTrue(holds.isHeldByUser(screeningId, "A1", userId));
    }

    @Test
    void lostRedisClaimResponse_isRecoveredOnRetry() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw redisFailure();
        }).when(holds).claimForBooking(screeningId, "A1", userId);
        book(userId).andExpect(status().isServiceUnavailable());
        assertEquals(0, bookings.count());
        assertEquals(-1L, redisTemplate.getExpire(key(), TimeUnit.MILLISECONDS));

        reset(holds);
        book(userId).andExpect(status().isCreated());
        assertFalse(holds.isHeld(screeningId, "A1"));
    }

    @Test
    void redisFailureBeforeCommit_rollsBackBooking() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        doThrow(redisFailure()).when(holds).verifyBookingHold(eq(screeningId), eq("A1"), anyString());
        book(userId).andExpect(status().isServiceUnavailable());
        assertEquals(0, bookings.count());
        assertTrue(holds.isHeldByUser(screeningId, "A1", userId));
    }

    @Test
    void holdExpiresBetweenValidationAndClaim_isConflict() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        doAnswer(invocation -> {
            Object expiry = invocation.callRealMethod();
            redisTemplate.expireAt(key(), Instant.EPOCH);
            return expiry;
        }).when(holds).getHoldExpiresAt(screeningId, "A1", userId);

        book(userId).andExpect(status().isConflict());
        assertEquals(0, bookings.count());
        assertFalse(holds.isHeld(screeningId, "A1"));
    }

    @Test
    void unexpectedRedisReplacementBeforeCommit_isDetectedAndPreserved() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        doAnswer(invocation -> {
            redisTemplate.opsForValue().set(key(), otherUserId.toString(), Duration.ofSeconds(30));
            return invocation.callRealMethod();
        }).when(holds).verifyBookingHold(eq(screeningId), eq("A1"), anyString());

        book(userId).andExpect(status().isConflict());
        assertEquals(0, bookings.count());
        assertTrue(holds.isHeldByUser(screeningId, "A1", otherUserId));
    }

    @Test
    void cleanupFailureAfterCommit_keepsSuccessfulResponseAndRecoversFromDatabase() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        doThrow(redisFailure()).when(holds).completeBookingHold(eq(screeningId), eq("A1"), anyString(), eq(true));
        book(userId).andExpect(status().isCreated());
        assertEquals(1, bookings.count());
        assertAvailability("BOOKED");
        assertEquals(-1L, redisTemplate.getExpire(key(), TimeUnit.MILLISECONDS));

        reset(holds);
        acquire(otherUserId).andExpect(status().isConflict());
        assertFalse(holds.isHeld(screeningId, "A1"));
    }

    @Test
    void cleanupFailureAfterRollback_restoresOriginalExpiryOnRetry() throws Exception {
        acquire(userId).andExpect(status().isCreated());
        Instant expiry = holds.getHoldExpiresAt(screeningId, "A1", userId).orElseThrow();
        doThrow(redisFailure()).when(holds).completeBookingHold(eq(screeningId), eq("A1"), anyString(), eq(false));
        transactions.executeWithoutResult(status -> {
            bookingService.createBooking(request(userId));
            status.setRollbackOnly();
        });
        assertEquals(0, bookings.count());
        assertEquals(-1L, redisTemplate.getExpire(key(), TimeUnit.MILLISECONDS));

        reset(holds);
        acquire(otherUserId).andExpect(status().isConflict());
        assertEquals(expiry, holds.getHoldExpiresAt(screeningId, "A1", userId).orElseThrow());
    }

    private Booking request(Long ownerId) {
        Screening screening = new Screening();
        screening.setId(screeningId);
        User user = new User();
        user.setId(ownerId);
        Seat seat = new Seat();
        seat.setSeatId(new SeatId("A1", roomId));
        return TestDataFactory.createBooking(screening, user, seat);
    }

    private ResultActions acquire(Long ownerId) throws Exception {
        return mockMvc.perform(post("/api/screenings/{id}/seats/A1/hold", screeningId)
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":%d}".formatted(ownerId)));
    }

    private ResultActions book(Long ownerId) throws Exception {
        return mockMvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"screeningId\":%d,\"userId\":%d,\"seatLabel\":\"A1\",\"roomId\":%d}"
                        .formatted(screeningId, ownerId, roomId)));
    }

    private void assertAvailability(String expected) throws Exception {
        mockMvc.perform(get("/api/screenings/{id}/seats", screeningId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value(expected));
    }

    private void awaitExpiry() throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (holds.isHeld(screeningId, "A1") && System.nanoTime() < deadline) Thread.sleep(10);
        assertFalse(holds.isHeld(screeningId, "A1"));
    }

    private RedisConnectionFailureException redisFailure() {
        return new RedisConnectionFailureException("Simulated Redis outage");
    }

    private String key() { return "seat-hold:" + screeningId + ":A1"; }
}
