package com.rayan.cinemaapi.service;

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

    @InjectMocks
    private BookingService bookingService;

    @Test
    void getBooking_returnsBookingWhenFound() {
        Booking booking = new Booking();
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

        Screening screening = new Screening();
        screening.setRoom(room);

        Seat seat = new Seat();
        seat.setRoom(room);

        Booking booking = new Booking();
        booking.setScreening(screening);
        booking.setSeat(seat);

        when(bookingRepository.save(booking))
                .thenReturn(booking);

        Booking result = bookingService.createBooking(booking);

        assertSame(booking, result);
        verify(bookingRepository).save(booking);
    }

    @Test
    void createBooking_throwsExceptionWhenSeatDoesNotBelongToScreeningRoom() {
        Room screeningRoom = new Room();
        screeningRoom.setId(1L);

        Room seatRoom = new Room();
        seatRoom.setId(2L);

        Screening screening = new Screening();
        screening.setRoom(screeningRoom);

        Seat seat = new Seat();
        seat.setRoom(seatRoom);

        Booking booking = new Booking();
        booking.setScreening(screening);
        booking.setSeat(seat);

        assertThrows(
                InvalidBookingException.class,
                () -> bookingService.createBooking(booking)
        );

        verify(bookingRepository, never()).save(booking);
    }

    @Test
    void updateBooking_updatesFields() {
        User oldUser = new User();
        User newUser = new User();

        Screening oldScreening = new Screening();
        Screening newScreening = new Screening();

        Seat oldSeat = new Seat();
        Seat newSeat = new Seat();

        Booking existingBooking = new Booking();
        existingBooking.setId(1L);
        existingBooking.setUser(oldUser);
        existingBooking.setScreening(oldScreening);
        existingBooking.setSeat(oldSeat);

        Booking updatedBooking = new Booking();
        updatedBooking.setId(1L);
        updatedBooking.setUser(newUser);
        updatedBooking.setScreening(newScreening);
        updatedBooking.setSeat(newSeat);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(existingBooking));

        Booking result = bookingService.updateBooking(updatedBooking);

        assertSame(existingBooking, result);
        assertSame(newUser, result.getUser());
        assertSame(newScreening, result.getScreening());
        assertSame(newSeat, result.getSeat());
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
    }

    @Test
    void deleteBooking_deletesBookingById() {
        bookingService.deleteBooking(1L);

        verify(bookingRepository).deleteById(1L);
    }
}