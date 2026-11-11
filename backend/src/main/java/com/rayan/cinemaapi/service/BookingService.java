package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Booking;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.User;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.exception.InvalidBookingException;
import com.rayan.cinemaapi.exception.SeatHoldConflictException;
import com.rayan.cinemaapi.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Objects;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private final BookingRepository bookingRepository;
    private final ScreeningService screeningService;
    private final UserService userService;
    private final SeatService seatService;
    private final SeatHoldService seatHoldService;

    public BookingService(
            BookingRepository bookingRepository,
            ScreeningService screeningService,
            UserService userService,
            SeatService seatService,
            SeatHoldService seatHoldService
    ) {
        this.bookingRepository = bookingRepository;
        this.screeningService = screeningService;
        this.userService = userService;
        this.seatService = seatService;
        this.seatHoldService = seatHoldService;
    }

    public List<Booking> getBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBooking(Long id) {
        return bookingRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Booking", id)
        );
    }

    @Transactional
    public Booking createBooking(Booking booking) {
        resolveBooking(booking);
        Long screeningId = booking.getScreening().getId();
        var seatId = booking.getSeat().getSeatId();
        String seatLabel = seatId.getSeatLabel();
        seatService.prepareSeatForBooking(screeningId, seatId);
        if (bookingRepository.existsByScreeningAndSeat(screeningId, seatLabel, seatId.getRoomId())) {
            throw new SeatHoldConflictException("Seat is already booked for this screening.");
        }
        String marker = seatHoldService.claimForBooking(screeningId, seatLabel, booking.getUser().getId());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                seatHoldService.verifyBookingHold(screeningId, seatLabel, marker);
            }

            @Override
            public void afterCompletion(int status) {
                // An unknown commit outcome must be resolved from PostgreSQL on
                // the next request rather than restoring a possibly booked hold.
                if (status == STATUS_UNKNOWN) {
                    log.warn("Booking outcome unknown for screening {}, seat {}; hold recovery deferred",
                            screeningId, seatLabel);
                    return;
                }
                try {
                    seatHoldService.completeBookingHold(screeningId, seatLabel, marker, status == STATUS_COMMITTED);
                } catch (RuntimeException exception) {
                    // Never report a committed booking as failed because Redis
                    // cleanup failed. Recovery runs under the next SQL seat lock.
                    log.warn("Redis hold cleanup failed for screening {}, seat {}; recovery deferred",
                            screeningId, seatLabel, exception);
                }
            }
        });
        return bookingRepository.saveAndFlush(booking);
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
