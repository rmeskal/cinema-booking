package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.TestDataFactory;
import com.rayan.cinemaapi.entity.Booking;
import com.rayan.cinemaapi.entity.Movie;
import com.rayan.cinemaapi.entity.Role;
import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.User;
import com.rayan.cinemaapi.exception.EntityInUseException;
import com.rayan.cinemaapi.repository.BookingRepository;
import com.rayan.cinemaapi.repository.MovieRepository;
import com.rayan.cinemaapi.repository.RoomRepository;
import com.rayan.cinemaapi.repository.RoomTypeRepository;
import com.rayan.cinemaapi.repository.ScreeningRepository;
import com.rayan.cinemaapi.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class EntityInUseTests {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MovieService movieService;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ScreeningService screeningService;

    @Autowired
    private UserService userService;

    @Autowired
    private SeatService seatService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void deleteEntities_throwsExceptionWhenEntitiesAreInUse() {
        RoomType roomType = roomTypeRepository.save(
                TestDataFactory.createRoomType("Standard")
        );

        Room room = roomRepository.save(
                TestDataFactory.createRoom(
                        "Test Room",
                        roomType
                )
        );

        Seat seat = TestDataFactory.createSeat(
                room,
                "A1"
        );

        entityManager.persist(seat);

        Movie movie = movieRepository.save(
                TestDataFactory.createMovie(
                        "Test Movie",
                        "A test movie",
                        120
                )
        );

        User user = userRepository.save(
                TestDataFactory.createUser(
                        "Test",
                        "User",
                        "test@example.com",
                        "hashed-password",
                        Role.USER
                )
        );

        Screening screening = screeningRepository.save(
                TestDataFactory.createScreening(
                        movie,
                        room,
                        LocalDateTime.of(2026, 8, 25, 19, 0)
                )
        );

        Booking booking = bookingRepository.save(
                TestDataFactory.createBooking(
                        screening,
                        user,
                        seat
                )
        );

        entityManager.flush();

        assertThrows(
                EntityInUseException.class,
                () -> roomTypeService.deleteRoomType(roomType.getId())
        );

        assertThrows(
                EntityInUseException.class,
                () -> roomService.deleteRoom(room.getId())
        );

        assertThrows(
                EntityInUseException.class,
                () -> movieService.deleteMovie(movie.getId())
        );

        assertThrows(
                EntityInUseException.class,
                () -> screeningService.deleteScreening(screening.getId())
        );

        assertThrows(
                EntityInUseException.class,
                () -> userService.deleteUser(user.getId())
        );

        assertThrows(
                EntityInUseException.class,
                () -> seatService.deleteSeat(seat.getSeatId())
        );

        assert booking.getId() != null;
    }
}