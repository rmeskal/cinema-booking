package com.rayan.cinemaapi.repository;

import com.rayan.cinemaapi.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByUserId(Long id);

    boolean existsByScreeningId(Long id);

    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM bookings
            WHERE seat_label = :seatLabel
              AND room_id = :roomId
        )
        """, nativeQuery = true)
    boolean existsBySeatId(
            @Param("seatLabel") String seatLabel,
            @Param("roomId") Long roomId
    );
}