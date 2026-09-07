package com.rayan.cinemaapi.repository;

import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.SeatId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, SeatId> {
    boolean existsByRoomId(Long id);

    List<Seat> findAllByRoomId(Long roomId);
}
