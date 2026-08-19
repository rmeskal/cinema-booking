package com.rayan.cinemaapi.repository;

import com.rayan.cinemaapi.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}
