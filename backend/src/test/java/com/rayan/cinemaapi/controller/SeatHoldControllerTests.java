package com.rayan.cinemaapi.controller;

import com.jayway.jsonpath.JsonPath;
import com.rayan.cinemaapi.TestDataFactory;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.User;
import com.rayan.cinemaapi.repository.BookingRepository;
import com.rayan.cinemaapi.repository.MovieRepository;
import com.rayan.cinemaapi.repository.RoomRepository;
import com.rayan.cinemaapi.repository.RoomTypeRepository;
import com.rayan.cinemaapi.repository.ScreeningRepository;
import com.rayan.cinemaapi.repository.UserRepository;
import com.rayan.cinemaapi.service.SeatHoldService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "booking.seat-hold-duration=PT30S")
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class SeatHoldControllerTests {

    private static final String HOLD_URL = "/api/screenings/{id}/seats/{seatLabel}/hold";

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:8.10.1"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private RoomTypeRepository roomTypeRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private ScreeningRepository screeningRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private SeatHoldService seatHoldService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Screening screening;
    private Seat seat;
    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        var movie = movieRepository.save(TestDataFactory.createMovie());
        var roomType = roomTypeRepository.save(TestDataFactory.createRoomType());
        var room = roomRepository.save(TestDataFactory.createRoom(roomType));
        screening = screeningRepository.save(TestDataFactory.createScreening(movie, room));
        seat = TestDataFactory.createSeat(room, "A1");
        entityManager.persist(seat);
        user = userRepository.save(TestDataFactory.createUser());
        otherUser = TestDataFactory.createUser();
        otherUser.setEmail("other@example.com");
        otherUser = userRepository.save(otherUser);
    }

    @AfterEach
    void clearHolds() {
        // This Redis container is isolated; SQL rollback does not remove Redis writes.
        try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    @Test
    void holdSeat_returnsCreatedWithActualRedisExpiry() throws Exception {
        Instant before = Instant.now();

        String response = acquire(screening.getId(), "A1", user.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.screeningId").value(screening.getId()))
                .andExpect(jsonPath("$.roomId").value(screening.getRoom().getId()))
                .andExpect(jsonPath("$.seatLabel").value("A1"))
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.expiresAt").isString())
                .andReturn().getResponse().getContentAsString();

        Instant expiresAt = Instant.parse(JsonPath.read(response, "$.expiresAt"));
        assertEquals(expiresAt, seatHoldService.getHoldExpiresAt(screening.getId(), "A1", user.getId()).orElseThrow());
        assertTrue(expiresAt.isAfter(before));
        assertFalse(expiresAt.isAfter(Instant.now().plusSeconds(30)));
        assertTrue(seatHoldService.isHeldByUser(screening.getId(), "A1", user.getId()));
    }

    @Test
    void holdSeat_rejectsAnotherUserWithoutChangingOwnerOrExpiry() throws Exception {
        acquire(screening.getId(), "A1", user.getId()).andExpect(status().isCreated());
        Instant expiresAt = seatHoldService.getHoldExpiresAt(screening.getId(), "A1", user.getId()).orElseThrow();

        acquire(screening.getId(), "A1", otherUser.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already held")));

        assertEquals(expiresAt, seatHoldService.getHoldExpiresAt(screening.getId(), "A1", user.getId()).orElseThrow());
        assertFalse(seatHoldService.isHeldByUser(screening.getId(), "A1", otherUser.getId()));
    }

    @Test
    void holdSeat_repeatedRequestByOwnerDoesNotRenewExpiry() throws Exception {
        acquire(screening.getId(), "A1", user.getId()).andExpect(status().isCreated());
        assertTrue(redisTemplate.expire(key(), Duration.ofSeconds(5)));
        Instant expiresAt = seatHoldService.getHoldExpiresAt(screening.getId(), "A1", user.getId()).orElseThrow();

        acquire(screening.getId(), "A1", user.getId()).andExpect(status().isConflict());

        assertEquals(expiresAt, seatHoldService.getHoldExpiresAt(screening.getId(), "A1", user.getId()).orElseThrow());
    }

    @Test
    void holdSeat_rejectsBookedSeat() throws Exception {
        bookingRepository.save(TestDataFactory.createBooking(screening, user, seat));

        acquire(screening.getId(), "A1", otherUser.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already booked")));

        assertFalse(seatHoldService.isHeld(screening.getId(), "A1"));
    }

    @Test
    void holdSeat_allowsIndependentScreenings() throws Exception {
        var second = screeningRepository.save(TestDataFactory.createScreening(
                screening.getMovie(), screening.getRoom(), screening.getStartTime().plusHours(3)));
        bookingRepository.save(TestDataFactory.createBooking(screening, user, seat));
        assertTrue(seatHoldService.holdSeat(screening.getId(), "A1", user.getId()));

        acquire(second.getId(), "A1", otherUser.getId()).andExpect(status().isCreated());
        release(second.getId(), "A1", otherUser.getId()).andExpect(status().isNoContent());

        assertTrue(seatHoldService.isHeldByUser(screening.getId(), "A1", user.getId()));
    }

    @Test
    void releaseSeat_removesHoldAndRepeatedReleaseSucceeds() throws Exception {
        acquire(screening.getId(), "A1", user.getId()).andExpect(status().isCreated());

        release(screening.getId(), "A1", user.getId()).andExpect(status().isNoContent());
        release(screening.getId(), "A1", user.getId()).andExpect(status().isNoContent());

        assertFalse(seatHoldService.isHeld(screening.getId(), "A1"));
        acquire(screening.getId(), "A1", otherUser.getId()).andExpect(status().isCreated());
    }

    @Test
    void releaseSeat_rejectsWrongOwnerAndPreservesHold() throws Exception {
        acquire(screening.getId(), "A1", user.getId()).andExpect(status().isCreated());

        release(screening.getId(), "A1", otherUser.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("another user")));

        assertTrue(seatHoldService.isHeldByUser(screening.getId(), "A1", user.getId()));
    }

    @Test
    void expiredHold_canBeReleasedOrReacquiredWithoutDeletingNewOwnersHold() throws Exception {
        acquire(screening.getId(), "A1", user.getId()).andExpect(status().isCreated());
        assertTrue(redisTemplate.expire(key(), Duration.ofMillis(100)));
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (seatHoldService.isHeld(screening.getId(), "A1") && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertFalse(seatHoldService.isHeld(screening.getId(), "A1"));

        release(screening.getId(), "A1", user.getId()).andExpect(status().isNoContent());
        acquire(screening.getId(), "A1", otherUser.getId()).andExpect(status().isCreated());
        release(screening.getId(), "A1", user.getId()).andExpect(status().isConflict());

        assertTrue(seatHoldService.isHeldByUser(screening.getId(), "A1", otherUser.getId()));
    }

    @Test
    void holdEndpoints_rejectUnknownScreening() throws Exception {
        acquire(999999L, "A1", user.getId()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        release(999999L, "A1", user.getId()).andExpect(status().isNotFound());
    }

    @Test
    void holdEndpoints_rejectUnknownUser() throws Exception {
        acquire(screening.getId(), "A1", 999999L).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("User")));
        release(screening.getId(), "A1", 999999L).andExpect(status().isNotFound());
        assertFalse(seatHoldService.isHeld(screening.getId(), "A1"));
    }

    @Test
    void holdEndpoints_rejectUnknownSeat() throws Exception {
        acquire(screening.getId(), "Z99", user.getId()).andExpect(status().isNotFound());
        release(screening.getId(), "Z99", user.getId()).andExpect(status().isNotFound());
        assertFalse(seatHoldService.isHeld(screening.getId(), "Z99"));
    }

    @Test
    void holdEndpoints_rejectSeatThatExistsOnlyInAnotherRoom() throws Exception {
        var otherRoom = roomRepository.save(TestDataFactory.createRoom("Other room", screening.getRoom().getRoomType()));
        entityManager.persist(TestDataFactory.createSeat(otherRoom, "B1"));

        acquire(screening.getId(), "B1", user.getId()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Seat")));
        release(screening.getId(), "B1", user.getId()).andExpect(status().isNotFound());

        assertFalse(seatHoldService.isHeld(screening.getId(), "B1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"userId\":null}", "{\"userId\":0}", "{\"userId\":-1}",
            "{\"userId\":\"invalid\"}", "not-json"})
    void holdSeat_rejectsInvalidBody(String body) throws Exception {
        mockMvc.perform(post(HOLD_URL, screening.getId(), "A1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        assertFalse(seatHoldService.isHeld(screening.getId(), "A1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "invalid"})
    void releaseSeat_rejectsInvalidUserId(String userId) throws Exception {
        mockMvc.perform(delete(HOLD_URL, screening.getId(), "A1").param("userId", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void releaseSeat_requiresUserId() throws Exception {
        mockMvc.perform(delete(HOLD_URL, screening.getId(), "A1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "invalid"})
    void holdEndpoints_rejectInvalidScreeningId(String id) throws Exception {
        mockMvc.perform(post(HOLD_URL, id, "A1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":%d}".formatted(user.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(delete(HOLD_URL, id, "A1").param("userId", user.getId().toString()))
                .andExpect(status().isBadRequest());
    }

    private ResultActions acquire(Long screeningId, String seatLabel, Long userId) throws Exception {
        return mockMvc.perform(post(HOLD_URL, screeningId, seatLabel).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":%d}".formatted(userId)));
    }

    private ResultActions release(Long screeningId, String seatLabel, Long userId) throws Exception {
        return mockMvc.perform(delete(HOLD_URL, screeningId, seatLabel).param("userId", userId.toString()));
    }

    private String key() {
        return "seat-hold:" + screening.getId() + ":A1";
    }
}
