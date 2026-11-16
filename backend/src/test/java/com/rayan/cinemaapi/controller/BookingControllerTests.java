package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.ApiTestSession;
import com.rayan.cinemaapi.TestDataFactory;
import com.rayan.cinemaapi.entity.Booking;
import com.rayan.cinemaapi.entity.Movie;
import com.rayan.cinemaapi.entity.Role;
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
import com.rayan.cinemaapi.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingControllerTests {

    // These tests cover request validation and mapping. BookingFlowTests exercises
    // real Redis ownership checks and real transaction completion separately.
    @MockitoBean
    private SeatHoldService seatHoldService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BookingRepository bookingRepository;

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
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private ApiTestSession client;

    @BeforeEach
    void signIn() throws Exception {
        String password = "test-password";
        user = userService.createUser(TestDataFactory.createUser(), password);
        client = ApiTestSession.signIn(mockMvc, user, password);
    }

    private Movie saveMovie() {
        return movieRepository.save(
                TestDataFactory.createMovie()
        );
    }

    private Room saveRoom() {
        return saveRoom(
                "Test Room",
                "Standard"
        );
    }

    private Room saveRoom(
            String name,
            String roomTypeName
    ) {
        RoomType roomType = roomTypeRepository.save(
                TestDataFactory.createRoomType(roomTypeName)
        );

        return roomRepository.save(
                TestDataFactory.createRoom(
                        name,
                        roomType
                )
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

    private User saveOtherUser() {
        return userRepository.save(TestDataFactory.createUser(
                "Another", "User", "other@example.com", "hashed-password", Role.USER));
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
    void bookingEndpoints_requireAuthentication() throws Exception {
        var anonymous = ApiTestSession.anonymous(mockMvc);

        mockMvc.perform(get("/api/bookings")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/bookings/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/bookings").with(anonymous::apply)).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/bookings/1").with(anonymous::apply)).andExpect(status().isUnauthorized());
    }

    @Test
    void bookingWrites_requireCsrf() throws Exception {
        mockMvc.perform(post("/api/bookings").session(client.session())).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/bookings/1").session(client.session())).andExpect(status().isForbidden());
    }

    @Test
    void usersCannotReadOrDeleteAnotherUsersBooking() throws Exception {
        Room room = saveRoom();
        Screening screening = screeningRepository.save(TestDataFactory.createScreening(saveMovie(), room));
        Booking booking = saveBooking(screening, saveOtherUser(), saveSeat(room, "A1"));

        mockMvc.perform(get("/api/bookings/{id}", booking.getId()).with(client::apply))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/bookings/{id}", booking.getId()).with(client::apply))
                .andExpect(status().isForbidden());
        assertTrue(bookingRepository.existsById(booking.getId()));
    }

    @Test
    void adminsCanListReadAndDeleteOtherUsersBookings() throws Exception {
        var admin = ApiTestSession.admin(mockMvc, userRepository, passwordEncoder);
        Room room = saveRoom();
        Screening screening = screeningRepository.save(TestDataFactory.createScreening(saveMovie(), room));
        Booking booking = saveBooking(screening, user, saveSeat(room, "A1"));
        saveBooking(screening, saveOtherUser(), saveSeat(room, "A2"));

        mockMvc.perform(get("/api/bookings").with(admin::apply))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/bookings/{id}", booking.getId()).with(admin::apply))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()));
        mockMvc.perform(delete("/api/bookings/{id}", booking.getId()).with(admin::apply))
                .andExpect(status().isNoContent());
        assertFalse(bookingRepository.existsById(booking.getId()));
    }

    @Test
    void getBookings_returnsOnlyOwnBookings() throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        Seat seat = saveSeat(room, "A1");

        Booking booking = saveBooking(
                screening,
                user,
                seat
        );

        saveBooking(screening, saveOtherUser(), saveSeat(room, "A2"));

        mockMvc.perform(get("/api/bookings").with(client::apply))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id")
                        .value(booking.getId()))
                .andExpect(jsonPath("$[0].screeningId")
                        .value(screening.getId()))
                .andExpect(jsonPath("$[0].userId")
                        .value(user.getId()))
                .andExpect(jsonPath("$[0].seatLabel")
                        .value("A1"))
                .andExpect(jsonPath("$[0].roomId")
                        .value(room.getId()));
    }

    @Test
    void getBookings_returnsEmptyListWhenNoBookingsExist()
            throws Exception {
        mockMvc.perform(get("/api/bookings").with(client::apply))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getBooking_returnsBookingWhenFound()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        Seat seat = saveSeat(room, "A1");

        Booking booking = saveBooking(
                screening,
                user,
                seat
        );

        mockMvc.perform(
                        get("/api/bookings/{id}", booking.getId()).with(client::apply)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(booking.getId()))
                .andExpect(jsonPath("$.screeningId")
                        .value(screening.getId()))
                .andExpect(jsonPath("$.userId")
                        .value(user.getId()))
                .andExpect(jsonPath("$.seatLabel")
                        .value("A1"))
                .andExpect(jsonPath("$.roomId")
                        .value(room.getId()));
    }

    @Test
    void getBooking_returns404WhenNotFound()
            throws Exception {
        mockMvc.perform(get("/api/bookings/999999").with(client::apply))
                .andExpect(status().isNotFound());
    }

    @Test
    void createBooking_createsBooking()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        Seat seat = saveSeat(room, "A1");

        mockMvc.perform(
                        post("/api/bookings").with(client::apply)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "screeningId": %d,
                                            "seatLabel": "A1",
                                            "roomId": %d
                                        }
                                        """.formatted(
                                        screening.getId(),
                                        room.getId()
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.screeningId")
                        .value(screening.getId()))
                .andExpect(jsonPath("$.userId")
                        .value(user.getId()))
                .andExpect(jsonPath("$.seatLabel")
                        .value("A1"))
                .andExpect(jsonPath("$.roomId")
                        .value(room.getId()));

        assertEquals(
                1,
                bookingRepository.count()
        );

        Booking savedBooking = bookingRepository.findAll()
                .getFirst();

        assertEquals(
                screening.getId(),
                savedBooking.getScreening().getId()
        );

        assertEquals(
                user.getId(),
                savedBooking.getUser().getId()
        );

        assertEquals(
                seat.getSeatId(),
                savedBooking.getSeat().getSeatId()
        );
    }

    @Test
    void createBooking_returns404WhenScreeningDoesNotExist()
            throws Exception {
        Room room = saveRoom();
        saveSeat(room, "A1");

        mockMvc.perform(
                        post("/api/bookings").with(client::apply)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "screeningId": 999999,
                                            "seatLabel": "A1",
                                            "roomId": %d
                                        }
                                        """.formatted(
                                        room.getId()
                                ))
                )
                .andExpect(status().isNotFound());

        assertEquals(
                0,
                bookingRepository.count()
        );
    }

    @Test
    void createBooking_returns404WhenSessionUserNoLongerExists()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        saveSeat(room, "A1");

        userRepository.delete(user);

        mockMvc.perform(
                        post("/api/bookings").with(client::apply)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "screeningId": %d,
                                            "seatLabel": "A1",
                                            "roomId": %d
                                        }
                                        """.formatted(
                                        screening.getId(),
                                        room.getId()
                                ))
                )
                .andExpect(status().isNotFound());

        assertEquals(
                0,
                bookingRepository.count()
        );
    }

    @Test
    void createBooking_returns404WhenSeatDoesNotExist()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        mockMvc.perform(
                        post("/api/bookings").with(client::apply)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "screeningId": %d,
                                            "seatLabel": "A1",
                                            "roomId": %d
                                        }
                                        """.formatted(
                                        screening.getId(),
                                        room.getId()
                                ))
                )
                .andExpect(status().isNotFound());

        assertEquals(
                0,
                bookingRepository.count()
        );
    }

    @Test
    void createBooking_returns409WhenSeatBelongsToDifferentRoom()
            throws Exception {
        Movie movie = saveMovie();

        Room screeningRoom = saveRoom(
                "Screening Room",
                "Standard"
        );

        Room seatRoom = saveRoom(
                "Seat Room",
                "Premium"
        );

        Screening screening = saveScreening(
                movie,
                screeningRoom,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        saveSeat(seatRoom, "A1");

        mockMvc.perform(
                        post("/api/bookings").with(client::apply)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "screeningId": %d,
                                            "seatLabel": "A1",
                                            "roomId": %d
                                        }
                                        """.formatted(
                                        screening.getId(),
                                        seatRoom.getId()
                                ))
                )
                .andExpect(status().isConflict());

        assertEquals(
                0,
                bookingRepository.count()
        );
    }

    @Test
    void createBooking_returns409WhenSeatIsAlreadyBooked()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        User firstUser = saveOtherUser();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        Seat seat = saveSeat(room, "A1");

        saveBooking(
                screening,
                firstUser,
                seat
        );

        mockMvc.perform(
                        post("/api/bookings").with(client::apply)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "screeningId": %d,
                                            "seatLabel": "A1",
                                            "roomId": %d
                                        }
                                        """.formatted(
                                        screening.getId(),
                                        room.getId()
                                ))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void createBooking_returns400WhenRequestIsInvalid()
            throws Exception {
        mockMvc.perform(
                        post("/api/bookings").with(client::apply)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "screeningId": null,
                                            "seatLabel": "",
                                            "roomId": null
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        assertEquals(
                0,
                bookingRepository.count()
        );
    }

    @Test
    void deleteBooking_deletesBooking()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        Seat seat = saveSeat(room, "A1");

        Booking booking = saveBooking(
                screening,
                user,
                seat
        );

        mockMvc.perform(
                        delete(
                                "/api/bookings/{id}",
                                booking.getId()
                        ).with(client::apply)
                )
                .andExpect(status().isNoContent());

        assertTrue(
                bookingRepository.findById(
                        booking.getId()
                ).isEmpty()
        );
    }

    @Test
    void deleteBooking_returns404WhenNotFound()
            throws Exception {
        mockMvc.perform(delete("/api/bookings/999999").with(client::apply))
                .andExpect(status().isNotFound());
    }
}
