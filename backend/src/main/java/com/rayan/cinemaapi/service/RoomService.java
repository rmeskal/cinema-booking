package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.SeatId;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeService roomTypeService;

    private static final int SEAT_ROWS = 6;
    private static final int SEATS_PER_ROW = 10;

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

    @Transactional
    public Room createRoom(Room room) {
        RoomType roomType = roomTypeService.getRoomType(room.getRoomType().getId());

        // Only the id of the roomtype gets supplied in a request
        // The following line gets used to fill in the other attributes of the roomtype
        room.setRoomType(roomType);

        Room savedRoom = roomRepository.save(room);
        createSeats(savedRoom);

        return savedRoom;
    }

    private void createSeats(Room room) {
        for (int row = 0; row < SEAT_ROWS; row++) {
            char rowLabel = (char) ('A' + row);

            for (int number = 1; number <= SEATS_PER_ROW; number++) {
                String seatLabel = rowLabel + String.valueOf(number);

                SeatId seatId = new SeatId(seatLabel, room.getId());

                Seat seat = new Seat();
                seat.setSeatId(seatId);
                seat.setRoom(room);
            }
        }
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
