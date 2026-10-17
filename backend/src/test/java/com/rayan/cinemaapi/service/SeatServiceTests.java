package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.SeatId;
import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.entity.SeatStatus;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.BookingRepository;
import com.rayan.cinemaapi.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceTests {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ScreeningService screeningService;

    @Mock
    private SeatHoldService seatHoldService;

    @InjectMocks
    private SeatService seatService;

    @Test
    void getSeatAvailability_checksBookingsBeforeHolds() {
        Room room = new Room();
        room.setId(2L);
        Screening screening = new Screening();
        screening.setRoom(room);
        Seat booked = new Seat();
        booked.setSeatId(new SeatId("A1", 2L));
        Seat held = new Seat();
        held.setSeatId(new SeatId("A2", 2L));
        Seat available = new Seat();
        available.setSeatId(new SeatId("A3", 2L));
        when(screeningService.getScreening(1L)).thenReturn(screening);
        when(seatRepository.findAllByRoomId(2L)).thenReturn(List.of(booked, held, available));
        when(bookingRepository.existsByScreeningAndSeat(1L, "A1", 2L)).thenReturn(true);
        when(seatHoldService.isHeld(1L, "A2")).thenReturn(true);

        var result = seatService.getSeatAvailability(1L);

        assertEquals(List.of(SeatStatus.BOOKED, SeatStatus.HELD, SeatStatus.AVAILABLE),
                result.stream().map(seat -> seat.status()).toList());
        verify(seatHoldService, never()).isHeld(1L, "A1");
        verify(seatHoldService).isHeld(1L, "A3");
    }

    @Test
    void getSeat_returnsSeatWhenFound() {
        SeatId seatId = new SeatId("A1", 1L);
        Seat seat = new Seat();
        seat.setSeatId(seatId);

        when(seatRepository.findById(seatId))
                .thenReturn(Optional.of(seat));

        Seat result = seatService.getSeat(seatId);

        assertSame(seat, result);
    }

    @Test
    void getSeat_throwsExceptionWhenNotFound() {
        SeatId seatId = new SeatId("A1", 1L);

        when(seatRepository.findById(seatId))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> seatService.getSeat(seatId)
        );
    }

    @Test
    void deleteSeat_deletesSeatById() {
        SeatId seatId = new SeatId("A1", 1L);

        when(seatRepository.existsById(seatId))
                .thenReturn(true);

        seatService.deleteSeat(seatId);

        verify(seatRepository).deleteById(seatId);
    }

    @Test
    void deleteSeat_throwsExceptionWhenSeatDoesNotExist() {
        SeatId seatId = new SeatId("A1", 1L);

        when(seatRepository.existsById(seatId))
                .thenReturn(false);

        assertThrows(
                EntityNotFoundException.class,
                () -> seatService.deleteSeat(seatId)
        );

        verify(seatRepository, never()).deleteById(seatId);
    }
}
