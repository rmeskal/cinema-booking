package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.RoomTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomTypeServiceTests {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private RoomTypeService roomTypeService;

    @Test
    void getRoomType_throwsExceptionWhenNotFound() {
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> roomTypeService.getRoomType(1L)
        );
    }

    @Test
    void updateRoomType_updatesName() {
        RoomType existingRoomType = new RoomType();
        existingRoomType.setId(1L);
        existingRoomType.setName("Standard");

        RoomType updatedRoomType = new RoomType();
        updatedRoomType.setId(1L);
        updatedRoomType.setName("IMAX");

        when(roomTypeRepository.findById(1L))
                .thenReturn(Optional.of(existingRoomType));

        RoomType result = roomTypeService.updateRoomType(updatedRoomType);

        assertEquals("IMAX", result.getName());
    }
}