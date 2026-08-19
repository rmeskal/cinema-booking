package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    public List<RoomType> getRoomTypes() {
        return roomTypeRepository.findAll();
    }

    public RoomType getRoomType(Long id) {
        return roomTypeRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("RoomType", id)
        );
    }

    public RoomType createRoomType(RoomType roomType) {
        return roomTypeRepository.save(roomType);
    }

    @Transactional
    public RoomType updateRoomType(RoomType roomType) {
        RoomType existingRoomType = getRoomType(roomType.getId());

        existingRoomType.setName(roomType.getName());

        return existingRoomType;
    }

    public void deleteRoomType(Long id) {
        roomTypeRepository.deleteById(id);
    }
}
