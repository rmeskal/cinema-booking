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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private User saveUser() {
        return userRepository.save(
                TestDataFactory.createUser()
        );
    }

    private User saveUser(
            String firstName,
            String lastName,
            String email
    ) {
        return userRepository.save(
                TestDataFactory.createUser(
                        firstName,
                        lastName,
                        email,
                        "hashed-password",
                        com.rayan.cinemaapi.entity.Role.USER
                )
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
    void getBookings_returnsBookings() throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();
        User user = saveUser();

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

        mockMvc.perform(get("/api/bookings"))
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
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getBooking_returnsBookingWhenFound()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();
        User user = saveUser();

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
                        get(
                                "/api/bookings/{id}",
                                booking.getId()
                        )
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
        mockMvc.perform(get("/api/bookings/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createBooking_createsBooking()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();
        User user = saveUser();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        Seat seat = saveSeat(room, "A1");

        mockMvc.perform(
                        post("/api/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "screeningId": %d,
                                            "userId": %d,
                                            "seatLabel": "A1",
                                            "roomId": %d
                                        }
                                        """.formatted(
                                        screening.getId(),
                                        user.getId(),
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
        User user = saveUser();
        saveSeat(room, "A1");

        mockMvc.perform(
                        post("/api/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "screeningId": 999999,
                                            "userId": %d,
                                            "seatLabel": "A1",
                                            "roomId": %d
                                        }
                                        """.formatted(
                                        user.getId(),
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
    void createBooking_returns404WhenUserDoesNotExist()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        saveSeat(room, "A1");

        mockMvc.perform(
                        post("/api/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "screeningId": %d,
                                            "userId": 999999,
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
        User user = saveUser();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        mockMvc.perform(
                        post("/api/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "screeningId": %d,
                                            "userId": %d,
                                            "seatLabel": "A1",
                                            "roomId": %d
                                        }
                                        """.formatted(
                                        screening.getId(),
                                        user.getId(),
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

        User user = saveUser();

        Screening screening = saveScreening(
                movie,
                screeningRoom,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        saveSeat(seatRoom, "A1");

        mockMvc.perform(
                        post("/api/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "screeningId": %d,
                                            "userId": %d,
                                            "seatLabel": "A1",
                                            "roomId": %d
                                        }
                                        """.formatted(
                                        screening.getId(),
                                        user.getId(),
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

        User firstUser = saveUser();

        User secondUser = saveUser(
                "Another",
                "User",
                "another@example.com"
        );

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
                        post("/api/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "screeningId": %d,
                                            "userId": %d,
                                            "seatLabel": "A1",
                                            "roomId": %d
                                        }
                                        """.formatted(
                                        screening.getId(),
                                        secondUser.getId(),
                                        room.getId()
                                ))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void createBooking_returns400WhenRequestIsInvalid()
            throws Exception {
        mockMvc.perform(
                        post("/api/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "screeningId": null,
                                            "userId": null,
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
        User user = saveUser();

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
                        )
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
        mockMvc.perform(delete("/api/bookings/999999"))
                .andExpect(status().isNotFound());
    }
}