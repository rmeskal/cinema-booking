package com.rayan.cinemaapi.repository;

import com.rayan.cinemaapi.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
