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