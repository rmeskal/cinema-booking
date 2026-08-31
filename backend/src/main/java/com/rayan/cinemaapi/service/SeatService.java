package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.SeatId;
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

    public SeatService(SeatRepository seatRepository, BookingRepository bookingRepository) {
        this.seatRepository = seatRepository;
        this.bookingRepository = bookingRepository;
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
}
