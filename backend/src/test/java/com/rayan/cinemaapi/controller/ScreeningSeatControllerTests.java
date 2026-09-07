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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ScreeningSeatControllerTests {

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