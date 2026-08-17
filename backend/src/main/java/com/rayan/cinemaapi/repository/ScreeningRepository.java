package com.rayan.cinemaapi.repository;

import com.rayan.cinemaapi.entity.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    // Checks whether the screening overlaps with an existing screening in the same room
    @Query(value = """
            SELECT EXISTS (
                SELECT 1
                FROM screenings s
                JOIN movies m ON s.movie_id = m.id
                WHERE s.room_id = :roomId
                  AND s.start_time < :endTime
                  AND s.start_time + (m.duration_in_minutes * INTERVAL '1 minute') > :startTime
            )
            """, nativeQuery = true)
    boolean existsOverlappingScreening(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Checks for an overlapping screening in the same room, excluding the screening being updated
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM screenings s
            JOIN movies m ON s.movie_id = m.id
            WHERE s.room_id = :roomId
              AND s.id <> :screeningId
              AND s.start_time < :endTime
              AND s.start_time + (m.duration_in_minutes * INTERVAL '1 minute') > :startTime
        )
        """, nativeQuery = true)
    boolean existsOverlappingScreeningExcept(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("screeningId") Long screeningId
    );
}
