package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Seat;
import com.rayan.cinemaapi.entity.SeatId;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceTests {

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private SeatService seatService;

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