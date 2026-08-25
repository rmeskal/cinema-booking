package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.TestDataFactory;
import com.rayan.cinemaapi.entity.*;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.exception.InvalidBookingException;
import com.rayan.cinemaapi.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTests {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ScreeningService screeningService;

    @Mock
    private UserService userService;

    @Mock
    private SeatService seatService;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void getBooking_returnsBookingWhenFound() {
        Booking booking = TestDataFactory.createBooking(
                new Screening(),
                new User(),
                new Seat()
        );
        booking.setId(1L);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        Booking result = bookingService.getBooking(1L);

        assertSame(booking, result);
    }

    @Test
    void getBooking_throwsExceptionWhenNotFound() {
        when(bookingRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> bookingService.getBooking(1L)
        );
    }

    @Test
    void createBooking_savesBooking() {
        Room room = new Room();
        room.setId(1L);

        Movie movie = TestDataFactory.createMovie();

        Screening screening = TestDataFactory.createScreening(
                movie,
                room
        );
        screening.setId(1L);

        User user = TestDataFactory.createUser();
        user.setId(1L);

        Seat seat = TestDataFactory.createSeat(room, "A1");
        SeatId seatId = seat.getSeatId();

        Booking booking = TestDataFactory.createBooking(
                screening,
                user,
                seat
        );

        when(screeningService.getScreening(1L))
                .thenReturn(screening);

        when(userService.getUser(1L))
                .thenReturn(user);

        when(seatService.getSeat(seatId))
                .thenReturn(seat);

        when(bookingRepository.save(booking))
                .thenReturn(booking);

        Booking result = bookingService.createBooking(booking);

        assertSame(booking, result);

        verify(screeningService).getScreening(1L);
        verify(userService).getUser(1L);
        verify(seatService).getSeat(seatId);
        verify(bookingRepository).save(booking);
    }

    @Test
    void createBooking_throwsExceptionWhenSeatDoesNotBelongToScreeningRoom() {
        Room screeningRoom = new Room();
        screeningRoom.setId(1L);

        Room seatRoom = new Room();
        seatRoom.setId(2L);

        Screening screening = TestDataFactory.createScreening(
                TestDataFactory.createMovie(),
                screeningRoom
        );
        screening.setId(1L);

        User user = TestDataFactory.createUser();
        user.setId(1L);

        Seat seat = TestDataFactory.createSeat(
                seatRoom,
                "A1"
        );
        SeatId seatId = seat.getSeatId();

        Booking booking = TestDataFactory.createBooking(
                screening,
                user,
                seat
        );

        when(screeningService.getScreening(1L))
                .thenReturn(screening);

        when(userService.getUser(1L))
                .thenReturn(user);

        when(seatService.getSeat(seatId))
                .thenReturn(seat);

        assertThrows(
                InvalidBookingException.class,
                () -> bookingService.createBooking(booking)
        );

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void updateBooking_updatesFields() {
        Room room = new Room();
        room.setId(1L);

        Screening oldScreening = TestDataFactory.createScreening(
                TestDataFactory.createMovie(),
                room
        );
        oldScreening.setId(1L);

        Screening newScreening = TestDataFactory.createScreening(
                TestDataFactory.createMovie(),
                room
        );
        newScreening.setId(2L);

        User oldUser = TestDataFactory.createUser();
        oldUser.setId(1L);

        User newUser = TestDataFactory.createUser(
                "Updated",
                "User",
                "updated@example.com",
                "hashed-password",
                Role.USER
        );
        newUser.setId(2L);

        Seat oldSeat = TestDataFactory.createSeat(room, "A1");
        Seat newSeat = TestDataFactory.createSeat(room, "A2");

        Booking existingBooking = TestDataFactory.createBooking(
                oldScreening,
                oldUser,
                oldSeat
        );
        existingBooking.setId(1L);

        Booking updatedBooking = TestDataFactory.createBooking(
                newScreening,
                newUser,
                newSeat
        );
        updatedBooking.setId(1L);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(existingBooking));

        when(screeningService.getScreening(2L))
                .thenReturn(newScreening);

        when(userService.getUser(2L))
                .thenReturn(newUser);

        when(seatService.getSeat(newSeat.getSeatId()))
                .thenReturn(newSeat);

        Booking result = bookingService.updateBooking(updatedBooking);

        assertSame(existingBooking, result);
        assertSame(newUser, result.getUser());
        assertSame(newScreening, result.getScreening());
        assertSame(newSeat, result.getSeat());

        verify(screeningService).getScreening(2L);
        verify(userService).getUser(2L);
        verify(seatService).getSeat(newSeat.getSeatId());
    }

    @Test
    void updateBooking_throwsExceptionWhenSeatDoesNotBelongToScreeningRoom() {
        Room screeningRoom = new Room();
        screeningRoom.setId(1L);

        Room seatRoom = new Room();
        seatRoom.setId(2L);

        Screening screening = TestDataFactory.createScreening(
                TestDataFactory.createMovie(),
                screeningRoom
        );
        screening.setId(1L);

        User user = TestDataFactory.createUser();
        user.setId(1L);

        Seat seat = TestDataFactory.createSeat(
                seatRoom,
                "A1"
        );

        Booking existingBooking = new Booking();
        existingBooking.setId(1L);

        Booking updatedBooking = TestDataFactory.createBooking(
                screening,
                user,
                seat
        );
        updatedBooking.setId(1L);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(existingBooking));

        when(screeningService.getScreening(1L))
                .thenReturn(screening);

        when(userService.getUser(1L))
                .thenReturn(user);

        when(seatService.getSeat(seat.getSeatId()))
                .thenReturn(seat);

        assertThrows(
                InvalidBookingException.class,
                () -> bookingService.updateBooking(updatedBooking)
        );

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void updateBooking_throwsExceptionWhenNotFound() {
        Booking booking = new Booking();
        booking.setId(1L);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> bookingService.updateBooking(booking)
        );

        verifyNoInteractions(
                screeningService,
                userService,
                seatService
        );
    }

    @Test
    void deleteBooking_deletesBookingById() {
        when(bookingRepository.existsById(1L))
                .thenReturn(true);

        bookingService.deleteBooking(1L);

        verify(bookingRepository).deleteById(1L);
    }

    @Test
    void deleteBooking_throwsExceptionWhenBookingDoesNotExist() {
        when(bookingRepository.existsById(1L))
                .thenReturn(false);

        assertThrows(
                EntityNotFoundException.class,
                () -> bookingService.deleteBooking(1L)
        );

        verify(bookingRepository, never()).deleteById(1L);
    }
}