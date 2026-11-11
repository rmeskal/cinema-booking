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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    @Mock
    private SeatHoldService seatHoldService;

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

        when(bookingRepository.saveAndFlush(booking))
                .thenReturn(booking);
        when(seatHoldService.claimForBooking(1L, "A1", 1L)).thenReturn("marker");

        TransactionSynchronizationManager.initSynchronization();
        try {
            Booking result = bookingService.createBooking(booking);
            assertSame(booking, result);
            verify(seatHoldService, never()).completeBookingHold(any(), any(), any(), anyBoolean());
            var synchronization = TransactionSynchronizationManager.getSynchronizations().getFirst();
            synchronization.beforeCommit(false);
            synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            verify(seatHoldService).verifyBookingHold(1L, "A1", "marker");
            verify(seatHoldService).completeBookingHold(1L, "A1", "marker", true);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(screeningService).getScreening(1L);
        verify(userService).getUser(1L);
        verify(seatService).getSeat(seatId);
        verify(bookingRepository).saveAndFlush(booking);
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

        verify(bookingRepository, never()).saveAndFlush(any());
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
