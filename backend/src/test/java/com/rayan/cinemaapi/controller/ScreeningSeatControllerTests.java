package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.TestDataFactory;
import com.rayan.cinemaapi.entity.Booking;
import com.rayan.cinemaapi.entity.Movie;
import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.User;
import com.rayan.cinemaapi.repository.BookingRepository;
import com.rayan.cinemaapi.repository.MovieRepository;
import com.rayan.cinemaapi.repository.RoomRepository;
import com.rayan.cinemaapi.repository.RoomTypeRepository;
import com.rayan.cinemaapi.repository.ScreeningRepository;
import com.rayan.cinemaapi.repository.SeatRepository;
import com.rayan.cinemaapi.repository.UserRepository;
import com.rayan.cinemaapi.service.SeatHoldService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
class ScreeningSeatControllerTests {

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
    void clearHolds() {
        // Redis writes are not rolled back by @Transactional. This container is test-only.
        try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
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
    private SeatRepository seatRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    private Movie saveMovie() {
        return movieRepository.save(
                TestDataFactory.createMovie()
        );
    }

    private Room saveRoom() {
        RoomType roomType = roomTypeRepository.save(
                TestDataFactory.createRoomType()
        );

        return roomRepository.save(
                TestDataFactory.createRoom(roomType)
        );
    }

    private Screening saveScreening(
            Movie movie,
            Room room,
            LocalDateTime startTime
    ) {
        return screeningRepository.save(
                TestDataFactory.createScreening(
                        movie,
                        room,
                        startTime
                )
        );
    }

    private User saveUser() {
        return userRepository.save(
                TestDataFactory.createUser()
        );
    }

    private Seat saveSeat(
            Room room,
            String seatLabel
    ) {
        Seat seat = TestDataFactory.createSeat(
                room,
                seatLabel
        );

        entityManager.persist(seat);
        return seat;
    }

    private Booking saveBooking(
            Screening screening,
            User user,
            Seat seat
    ) {
        return bookingRepository.save(
                TestDataFactory.createBooking(
                        screening,
                        user,
                        seat
                )
        );
    }

    @Test
    void getSeatAvailability_returnsBookedHeldAndAvailableSeats() throws Exception {
        Room room = saveRoom();
        Screening screening = saveScreening(saveMovie(), room, LocalDateTime.of(2026, 8, 25, 19, 0));
        Seat bookedSeat = saveSeat(room, "A1");
        saveSeat(room, "A2");
        saveSeat(room, "A3");
        var user = userRepository.save(TestDataFactory.createUser());
        bookingRepository.save(TestDataFactory.createBooking(screening, user, bookedSeat));
        assertTrue(seatHoldService.holdSeat(screening.getId(), "A1", user.getId()));
        assertTrue(seatHoldService.holdSeat(screening.getId(), "A2", user.getId()));

        mockMvc.perform(get("/api/screenings/{id}/seats", screening.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.seatLabel == 'A1')].status").value(contains("BOOKED")))
                .andExpect(jsonPath("$[?(@.seatLabel == 'A2')].status").value(contains("HELD")))
                .andExpect(jsonPath("$[?(@.seatLabel == 'A3')].status").value(contains("AVAILABLE")));
    }

    @Test
    void getSeatAvailability_returnsAvailableAfterHoldIsReleased() throws Exception {
        Screening screening = saveScreeningWithSeat();
        assertTrue(seatHoldService.holdSeat(screening.getId(), "A1", 10L));
        assertSeatStatus(screening, "HELD");

        assertTrue(seatHoldService.releaseSeat(screening.getId(), "A1", 10L));

        assertSeatStatus(screening, "AVAILABLE");
    }

    @Test
    void getSeatAvailability_returnsAvailableAfterHoldExpires() throws Exception {
        Screening screening = saveScreeningWithSeat();
        assertTrue(seatHoldService.holdSeat(screening.getId(), "A1", 10L));
        assertSeatStatus(screening, "HELD");
        assertTrue(redisTemplate.expire("seat-hold:" + screening.getId() + ":A1", Duration.ofMillis(100)));

        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (seatHoldService.isHeld(screening.getId(), "A1") && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertFalse(seatHoldService.isHeld(screening.getId(), "A1"),
                "Hold should expire in Redis");

        assertSeatStatus(screening, "AVAILABLE");
    }

    @Test
    void getSeatAvailability_ignoresHoldsAndBookingsFromAnotherScreening() throws Exception {
        Screening first = saveScreeningWithSeat();
        Screening second = saveScreening(first.getMovie(), first.getRoom(), first.getStartTime().plusHours(3));
        var user = userRepository.save(TestDataFactory.createUser());
        var seat = seatRepository.findAllByRoomId(first.getRoom().getId()).getFirst();
        bookingRepository.save(TestDataFactory.createBooking(first, user, seat));
        assertTrue(seatHoldService.holdSeat(first.getId(), "A1", user.getId()));

        assertSeatStatus(second, "AVAILABLE");
        assertTrue(seatHoldService.holdSeat(second.getId(), "A1", user.getId()));
        assertSeatStatus(second, "HELD");
        assertSeatStatus(first, "BOOKED");
    }

    private Screening saveScreeningWithSeat() {
        Room room = saveRoom();
        saveSeat(room, "A1");
        return saveScreening(saveMovie(), room, LocalDateTime.of(2026, 8, 25, 19, 0));
    }

    private void assertSeatStatus(Screening screening, String expectedStatus) throws Exception {
        mockMvc.perform(get("/api/screenings/{id}/seats", screening.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].seatLabel").value("A1"))
                .andExpect(jsonPath("$[0].roomId").value(screening.getRoom().getId()))
                .andExpect(jsonPath("$[0].status").value(expectedStatus));
    }

    @Test
    void getSeatAvailability_returnsAllSeatsAsAvailableWhenNoBookingsExist()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        saveSeat(room, "A1");
        saveSeat(room, "A2");

        mockMvc.perform(
                        get(
                                "/api/screenings/{id}/seats",
                                screening.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].seatLabel").value("A1"))
                .andExpect(jsonPath("$[0].roomId").value(room.getId()))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[1].seatLabel").value("A2"))
                .andExpect(jsonPath("$[1].roomId").value(room.getId()))
                .andExpect(jsonPath("$[1].status").value("AVAILABLE"));
    }

    @Test
    void getSeatAvailability_returnsBookedForBookedSeat()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();
        User user = saveUser();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        Seat bookedSeat = saveSeat(room, "A1");
        saveSeat(room, "A2");

        saveBooking(
                screening,
                user,
                bookedSeat
        );

        mockMvc.perform(
                        get(
                                "/api/screenings/{id}/seats",
                                screening.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].seatLabel").value("A1"))
                .andExpect(jsonPath("$[0].status").value("BOOKED"))
                .andExpect(jsonPath("$[1].seatLabel").value("A2"))
                .andExpect(jsonPath("$[1].status").value("AVAILABLE"));
    }

    @Test
    void getSeatAvailability_bookingForAnotherScreeningDoesNotAffectSeat()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();
        User user = saveUser();

        Screening firstScreening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        Screening secondScreening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 22, 0)
        );

        Seat seat = saveSeat(room, "A1");

        saveBooking(
                firstScreening,
                user,
                seat
        );

        mockMvc.perform(
                        get(
                                "/api/screenings/{id}/seats",
                                secondScreening.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].seatLabel").value("A1"))
                .andExpect(jsonPath("$[0].roomId").value(room.getId()))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void getSeatAvailability_returns404WhenScreeningDoesNotExist()
            throws Exception {
        mockMvc.perform(
                        get("/api/screenings/999999/seats")
                )
                .andExpect(status().isNotFound());
    }
}
