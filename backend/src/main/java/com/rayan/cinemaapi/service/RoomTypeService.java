package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.exception.EntityInUseException;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.RoomRepository;
import com.rayan.cinemaapi.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository, RoomRepository roomRepository) {
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
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
        if (!roomTypeRepository.existsById(id)) {
            throw new EntityNotFoundException("RoomType", id);
        }

        if (roomRepository.existsByRoomTypeId(id)) {
            throw new EntityInUseException("RoomType", id);
        }

        roomTypeRepository.deleteById(id);
    }
}
