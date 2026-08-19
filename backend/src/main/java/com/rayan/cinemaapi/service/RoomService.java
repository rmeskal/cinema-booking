package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
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
        return roomRepository.save(room);
    }

    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }

    @Transactional
    public Room updateRoom(Room room) {
        Room existingRoom = getRoom(room.getId());

        existingRoom.setRoomType(room.getRoomType());

        return existingRoom;
    }
}
