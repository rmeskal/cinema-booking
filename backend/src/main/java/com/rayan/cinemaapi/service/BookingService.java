package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Booking;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.User;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.exception.InvalidBookingException;
import com.rayan.cinemaapi.repository.BookingRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ScreeningService screeningService;
    private final UserService userService;
    private final SeatService seatService;

    public BookingService(
            BookingRepository bookingRepository,
            ScreeningService screeningService,
            UserService userService,
            SeatService seatService
    ) {
        this.bookingRepository = bookingRepository;
        this.screeningService = screeningService;
        this.userService = userService;
        this.seatService = seatService;
    }

    public List<Booking> getBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBooking(Long id) {
        return bookingRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Booking", id)
        );
    }

    public Booking createBooking(Booking booking) {
        resolveBooking(booking);

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking updateBooking(Booking booking) {
        Booking existingBooking = getBooking(booking.getId());

        resolveBooking(booking);

        if (bookingRepository.existsByScreeningAndSeatExcept(
                booking.getScreening().getId(),
                booking.getSeat().getSeatId().getSeatLabel(),
                booking.getSeat().getSeatId().getRoomId(),
                booking.getId()
        )) {
            throw new InvalidBookingException();
        }

        existingBooking.setUser(booking.getUser());
        existingBooking.setScreening(booking.getScreening());
        existingBooking.setSeat(booking.getSeat());

        return existingBooking;
    }

    public void deleteBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new EntityNotFoundException("Booking", id);
        }

        bookingRepository.deleteById(id);
    }

    private void resolveBooking(Booking booking) {
        Screening screening = screeningService.getScreening(
                booking.getScreening().getId()
        );

        User user = userService.getUser(
                booking.getUser().getId()
        );

        Seat seat = seatService.getSeat(
                booking.getSeat().getSeatId()
        );

        if (!Objects.equals(
                screening.getRoom().getId(),
                seat.getRoom().getId()
        )) {
            throw new InvalidBookingException();
        }

        booking.setScreening(screening);
        booking.setUser(user);
        booking.setSeat(seat);
    }
}