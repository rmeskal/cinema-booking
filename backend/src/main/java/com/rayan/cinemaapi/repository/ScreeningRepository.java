package com.rayan.cinemaapi.repository;

import com.rayan.cinemaapi.entity.Screening;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {
}
