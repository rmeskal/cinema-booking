package com.rayan.cinemaapi.repository;

import com.rayan.cinemaapi.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}
