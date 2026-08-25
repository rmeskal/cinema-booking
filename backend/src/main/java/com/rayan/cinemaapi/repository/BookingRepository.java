package com.rayan.cinemaapi.repository;

import com.rayan.cinemaapi.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query(value = """
            SELECT COUNT(*) > 0
            FROM bookings
            WHERE screening_id = :screeningId
              AND seat_label = :seatLabel
              AND room_id = :roomId
              AND id <> :bookingId
            """, nativeQuery = true)
    boolean existsByScreeningAndSeatExcept(
            @Param("screeningId") Long screeningId,
            @Param("seatLabel") String seatLabel,
            @Param("roomId") Long roomId,
            @Param("bookingId") Long bookingId
    );
}