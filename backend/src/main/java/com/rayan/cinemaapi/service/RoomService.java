package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeService roomTypeService;

    public RoomService(RoomRepository roomRepository, RoomTypeService roomTypeService) {
        this.roomRepository = roomRepository;
        this.roomTypeService = roomTypeService;
    }

    public List<Room> getRooms(){
        return roomRepository.findAll();
    }

    public Room getRoom(Long id) {
        return roomRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Room", id)
        );
    }

    public Room createRoom(Room room) {
        RoomType roomType = roomTypeService.getRoomType(room.getRoomType().getId());

        // Only the id of the roomtype gets supplied in a request
        // The following line gets used to fill in the other attributes of the roomtype
        room.setRoomType(roomType);

        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new EntityNotFoundException("Room", id);
        }

        roomRepository.deleteById(id);
    }

    @Transactional
    public Room updateRoom(Room room) {
        Room existingRoom = getRoom(room.getId());

        RoomType roomType = roomTypeService.getRoomType(room.getRoomType().getId());

        existingRoom.setName(room.getName());
        existingRoom.setRoomType(roomType);

        return existingRoom;
    }
}
