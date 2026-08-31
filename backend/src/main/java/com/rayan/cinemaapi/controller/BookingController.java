package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.dto.booking.BookingRequest;
import com.rayan.cinemaapi.dto.booking.BookingResponse;
import com.rayan.cinemaapi.entity.Booking;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.SeatId;
import com.rayan.cinemaapi.entity.User;
import com.rayan.cinemaapi.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<BookingResponse> getBookings() {
        return bookingService.getBookings()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public BookingResponse getBooking(@PathVariable Long id) {
        return toResponse(bookingService.getBooking(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(
            @Valid @RequestBody BookingRequest request
    ) {
        return toResponse(
                bookingService.createBooking(toEntity(request))
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
    }

    private Booking toEntity(BookingRequest request) {
        Booking booking = new Booking();

        Screening screening = new Screening();
        screening.setId(request.screeningId());
        booking.setScreening(screening);

        User user = new User();
        user.setId(request.userId());
        booking.setUser(user);

        Seat seat = new Seat();
        seat.setSeatId(new SeatId(
                request.seatLabel(),
                request.roomId()
        ));
        booking.setSeat(seat);

        return booking;
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getScreening().getId(),
                booking.getUser().getId(),
                booking.getSeat().getSeatId().getSeatLabel(),
                booking.getSeat().getSeatId().getRoomId()
        );
    }
}