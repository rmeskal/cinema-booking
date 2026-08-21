package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.TestDataFactory;
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

    @Mock
    private RoomTypeService roomTypeService;

    @InjectMocks
    private RoomService roomService;

    @Test
    void getRoom_throwsExceptionWhenNotFound() {
        when(roomRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> roomService.getRoom(1L)
        );
    }

    @Test
    void updateRoom_updatesRoomType() {
        RoomType oldType = TestDataFactory.createRoomType("Standard");
        oldType.setId(1L);

        RoomType newType = TestDataFactory.createRoomType("IMAX");
        newType.setId(2L);

        Room existingRoom = TestDataFactory.createRoom(oldType);
        existingRoom.setId(1L);

        Room updatedRoom = TestDataFactory.createRoom(newType);
        updatedRoom.setId(1L);

        when(roomRepository.findById(1L))
                .thenReturn(Optional.of(existingRoom));

        when(roomTypeService.getRoomType(2L))
                .thenReturn(newType);

        Room result = roomService.updateRoom(updatedRoom);

        assertSame(newType, result.getRoomType());
    }
}