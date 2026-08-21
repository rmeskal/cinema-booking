package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.TestDataFactory;
import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.exception.EntityInUseException;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.RoomRepository;
import com.rayan.cinemaapi.repository.RoomTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomTypeServiceTests {

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomTypeService roomTypeService;

    @Test
    void getRoomType_throwsExceptionWhenNotFound() {
        when(roomTypeRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> roomTypeService.getRoomType(1L)
        );
    }

    @Test
    void updateRoomType_updatesName() {
        RoomType existingRoomType =
                TestDataFactory.createRoomType("Standard");
        existingRoomType.setId(1L);

        RoomType updatedRoomType =
                TestDataFactory.createRoomType("IMAX");
        updatedRoomType.setId(1L);

        when(roomTypeRepository.findById(1L))
                .thenReturn(Optional.of(existingRoomType));

        RoomType result =
                roomTypeService.updateRoomType(updatedRoomType);

        assertEquals("IMAX", result.getName());
    }

    @Test
    void deleteRoomType_throwsExceptionWhenInUse() {
        when(roomTypeRepository.existsById(1L))
                .thenReturn(true);

        when(roomRepository.existsByRoomTypeId(1L))
                .thenReturn(true);

        assertThrows(
                EntityInUseException.class,
                () -> roomTypeService.deleteRoomType(1L)
        );

        verify(roomRepository).existsByRoomTypeId(1L);
        verify(roomTypeRepository, never()).deleteById(1L);
    }

    @Test
    void deleteRoomType_deletesWhenNotInUse() {
        when(roomTypeRepository.existsById(1L))
                .thenReturn(true);

        when(roomRepository.existsByRoomTypeId(1L))
                .thenReturn(false);

        roomTypeService.deleteRoomType(1L);

        verify(roomRepository).existsByRoomTypeId(1L);
        verify(roomTypeRepository).deleteById(1L);
    }
}