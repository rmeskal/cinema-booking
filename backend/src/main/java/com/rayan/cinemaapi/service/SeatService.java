package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.SeatId;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.SeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatService {

    private final SeatRepository seatRepository;

    public SeatService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
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
        seatRepository.deleteById(id);
    }
}
