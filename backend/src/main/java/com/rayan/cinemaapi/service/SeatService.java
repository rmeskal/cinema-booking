package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.dto.seat.SeatAvailabilityResponse;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.SeatId;
import com.rayan.cinemaapi.entity.SeatStatus;
import com.rayan.cinemaapi.exception.EntityInUseException;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.BookingRepository;
import com.rayan.cinemaapi.repository.SeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final ScreeningService screeningService;

    public SeatService(SeatRepository seatRepository, BookingRepository bookingRepository, ScreeningService screeningService) {
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
        this.screeningService = screeningService;
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

                    SeatStatus status = booked ? SeatStatus.BOOKED : SeatStatus.AVAILABLE;

                    return new SeatAvailabilityResponse(
                            seatId.getSeatLabel(),
                            roomId,
                            status
                    );
                })
                .toList();
    }
}
