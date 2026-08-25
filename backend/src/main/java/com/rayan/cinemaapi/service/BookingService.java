package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Booking;
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

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
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
        if (!Objects.equals(booking.getScreening().getRoom().getId(), booking.getSeat().getRoom().getId())) {
            throw new InvalidBookingException();
        }

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking updateBooking(Booking booking) {
        Booking existingBooking = getBooking(booking.getId());

        if (!Objects.equals(
                booking.getScreening().getRoom().getId(),
                booking.getSeat().getRoom().getId())) {
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
}
