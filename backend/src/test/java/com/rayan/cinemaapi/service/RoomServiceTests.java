package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTests {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomService roomService;

    @Test
    void getRoom_throwsExceptionWhenNotFound() {
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> roomService.getRoom(1L)
        );
    }

    @Test
    void updateRoom_updatesRoomType() {
        RoomType oldType = new RoomType();
        RoomType newType = new RoomType();

        Room existingRoom = new Room();
        existingRoom.setId(1L);
        existingRoom.setRoomType(oldType);

        Room updatedRoom = new Room();
        updatedRoom.setId(1L);
        updatedRoom.setRoomType(newType);

        when(roomRepository.findById(1L))
                .thenReturn(Optional.of(existingRoom));

        Room result = roomService.updateRoom(updatedRoom);

        assertSame(newType, result.getRoomType());
    }
}