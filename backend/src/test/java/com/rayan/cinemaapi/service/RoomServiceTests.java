package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.TestDataFactory;
import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.RoomRepository;
import com.rayan.cinemaapi.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTests {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeService roomTypeService;

    @InjectMocks
    private RoomService roomService;

    @Mock
    private SeatRepository seatRepository;

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
    void createRoom_createsSeats() {
        RoomType roomType = TestDataFactory.createRoomType("Standard");
        roomType.setId(1L);

        Room room = TestDataFactory.createRoom(roomType);

        when(roomTypeService.getRoomType(1L))
                .thenReturn(roomType);

        when(roomRepository.save(room))
                .thenAnswer(invocation -> {
                    Room savedRoom = invocation.getArgument(0);
                    savedRoom.setId(1L);
                    return savedRoom;
                });

        Room result = roomService.createRoom(room);

        assertNotNull(result.getId());

        ArgumentCaptor<Seat> seatCaptor = ArgumentCaptor.forClass(Seat.class);

        verify(seatRepository, times(60))
                .save(seatCaptor.capture());

        List<Seat> seats = seatCaptor.getAllValues();

        assertEquals(60, seats.size());
        assertEquals("A1", seats.getFirst().getSeatId().getSeatLabel());
        assertEquals("F10", seats.get(59).getSeatId().getSeatLabel());

        assertTrue(seats.stream()
                .allMatch(seat -> seat.getRoom() == result));
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