package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.dto.seat.SeatAvailabilityResponse;
import com.rayan.cinemaapi.dto.seat.SeatHoldResponse;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.SeatId;
import com.rayan.cinemaapi.entity.SeatStatus;
import com.rayan.cinemaapi.exception.EntityInUseException;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.exception.SeatHoldConflictException;
import com.rayan.cinemaapi.repository.BookingRepository;
import com.rayan.cinemaapi.repository.SeatRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final ScreeningService screeningService;
    private final SeatHoldService seatHoldService;
    private final UserService userService;

    public SeatService(SeatRepository seatRepository, BookingRepository bookingRepository,
                       ScreeningService screeningService, SeatHoldService seatHoldService,
                       UserService userService) {
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
        this.screeningService = screeningService;
        this.seatHoldService = seatHoldService;
        this.userService = userService;
    }

    public List<Seat> getSeats() {
        return seatRepository.findAll();
    }

    public Seat getSeat(SeatId id) {
        return seatRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Seat", id)
        );
    }

    public Seat createSeat(Seat seat) {
        return seatRepository.save(seat);
    }

    public void deleteSeat(SeatId id) {
        if (!seatRepository.existsById(id)) {
            throw new EntityNotFoundException("Seat", id);
        }

        if (bookingRepository.existsBySeatId(id.getSeatLabel(), id.getRoomId())) {
            throw new EntityInUseException("Seat", id);
        }

        seatRepository.deleteById(id);
    }

    public List<SeatAvailabilityResponse> getSeatAvailability(Long screeningId) {
        Screening screening = screeningService.getScreening(screeningId);

        Long roomId = screening.getRoom().getId();

        return seatRepository.findAllByRoomId(roomId).stream()
                .map(seat -> {
                    SeatId seatId = seat.getSeatId();

                    boolean booked = bookingRepository.existsByScreeningAndSeat(
                            screeningId,
                            seatId.getSeatLabel(),
                            roomId
                    );

                    SeatStatus status;
                    if (booked) {
                        status = SeatStatus.BOOKED;
                    } else if (seatHoldService.isHeld(screeningId, seatId.getSeatLabel())) {
                        status = SeatStatus.HELD;
                    } else {
                        status = SeatStatus.AVAILABLE;
                    }

                    return new SeatAvailabilityResponse(
                            seatId.getSeatLabel(),
                            roomId,
                            status
                    );
                })
                .toList();
    }

    public SeatHoldResponse holdSeat(Long screeningId, String seatLabel, Long userId) {
        SeatId seatId = validateSeatHoldRequest(screeningId, seatLabel, userId);
        if (bookingRepository.existsByScreeningAndSeat(screeningId, seatLabel, seatId.getRoomId())) {
            throw new SeatHoldConflictException("Seat is already booked for this screening.");
        }
        // SET NX is the authority for concurrent requests, including repeat requests by the owner.
        if (!seatHoldService.holdSeat(screeningId, seatLabel, userId)) {
            throw new SeatHoldConflictException("Seat is already held for this screening.");
        }
        Instant expiresAt = seatHoldService.getHoldExpiresAt(screeningId, seatLabel, userId)
                .orElseThrow(() -> new SeatHoldConflictException("Seat hold is no longer active. Please retry."));

        return new SeatHoldResponse(screeningId, seatLabel, seatId.getRoomId(), userId, expiresAt);
    }

    public void releaseSeatHold(Long screeningId, String seatLabel, Long userId) {
        validateSeatHoldRequest(screeningId, seatLabel, userId);
        if (!seatHoldService.releaseSeat(screeningId, seatLabel, userId)
                && seatHoldService.isHeld(screeningId, seatLabel)) {
            throw new SeatHoldConflictException("Seat hold belongs to another user.");
        }
        // An absent or expired hold is already released: DELETE is idempotent.
    }

    private SeatId validateSeatHoldRequest(Long screeningId, String seatLabel, Long userId) {
        Screening screening = screeningService.getScreening(screeningId);
        userService.getUser(userId);
        // Seats have composite IDs. Deriving the room from the screening ensures
        // that a seat from a different room can never be held through this endpoint.
        SeatId seatId = new SeatId(seatLabel, screening.getRoom().getId());
        return getSeat(seatId).getSeatId();
    }
}
