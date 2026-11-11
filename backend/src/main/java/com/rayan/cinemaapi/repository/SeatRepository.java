package com.rayan.cinemaapi.repository;

import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.SeatId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, SeatId> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.seatId = :id")
    Optional<Seat> findByIdForUpdate(SeatId id);

    boolean existsByRoomId(Long id);

    List<Seat> findAllByRoomId(Long roomId);
}
