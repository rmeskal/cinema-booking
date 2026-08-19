package com.rayan.cinemaapi.repository;


import com.rayan.cinemaapi.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
}
