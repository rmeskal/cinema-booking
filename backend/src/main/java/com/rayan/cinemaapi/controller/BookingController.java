package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.dto.booking.BookingRequest;
import com.rayan.cinemaapi.dto.booking.BookingResponse;
import com.rayan.cinemaapi.entity.Booking;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.SeatId;
import com.rayan.cinemaapi.entity.User;
import com.rayan.cinemaapi.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Manage bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    @Operation(summary = "List the signed-in user's bookings, or all bookings for admins")
    public List<BookingResponse> getBookings(Authentication authentication) {
        return bookingService.getBookings(Long.valueOf(authentication.getName()), isAdmin(authentication))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a booking by ID as its owner or an admin")
    public BookingResponse getBooking(@PathVariable Long id, Authentication authentication) {
        return toResponse(bookingService.getBooking(id, Long.valueOf(authentication.getName()), isAdmin(authentication)));
    }

    @PostMapping
    @Operation(summary = "Confirm a booking from the signed-in user's seat hold")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(
            @Valid @RequestBody BookingRequest request,
            Principal principal
    ) {
        return toResponse(
                bookingService.createBooking(toEntity(request, Long.valueOf(principal.getName())))
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a booking as its owner or an admin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBooking(@PathVariable Long id, Authentication authentication) {
        bookingService.deleteBooking(id, Long.valueOf(authentication.getName()), isAdmin(authentication));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private Booking toEntity(BookingRequest request, Long userId) {
        Booking booking = new Booking();

        Screening screening = new Screening();
        screening.setId(request.screeningId());
        booking.setScreening(screening);

        User user = new User();
        user.setId(userId);
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
