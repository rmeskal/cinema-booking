package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Movie;
import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.exception.ScreeningOverlapException;
import com.rayan.cinemaapi.repository.ScreeningRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScreeningServiceTests {

    @Mock
    private ScreeningRepository screeningRepository;

    @InjectMocks
    private ScreeningService screeningService;

    @Test
    void getScreening_returnsScreeningWhenFound() {
        Screening screening = new Screening();
        screening.setId(1L);

        when(screeningRepository.findById(1L))
                .thenReturn(Optional.of(screening));

        Screening result = screeningService.getScreening(1L);

        assertSame(screening, result);
    }

    @Test
    void getScreening_throwsExceptionWhenNotFound() {
        when(screeningRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> screeningService.getScreening(1L)
        );
    }

    @Test
    void createScreening_throwsExceptionWhenOverlapping() {
        Room room = new Room();
        room.setId(1L);

        Movie movie = new Movie();
        movie.setDurationInMinutes(120);

        Screening screening = new Screening();
        screening.setRoom(room);
        screening.setMovie(movie);
        screening.setStartTime(
                LocalDateTime.of(2026, 8, 17, 20, 0)
        );

        when(screeningRepository.existsOverlappingScreening(
                1L,
                screening.getStartTime(),
                screening.getEndTime()
        )).thenReturn(true);

        assertThrows(
                ScreeningOverlapException.class,
                () -> screeningService.createScreening(screening)
        );

        verify(screeningRepository, never()).save(screening);
    }

    @Test
    void createScreening_savesWhenNoOverlap() {
        Room room = new Room();
        room.setId(1L);

        Movie movie = new Movie();
        movie.setDurationInMinutes(120);

        Screening screening = new Screening();
        screening.setRoom(room);
        screening.setMovie(movie);
        screening.setStartTime(
                LocalDateTime.of(2026, 8, 17, 20, 0)
        );

        when(screeningRepository.existsOverlappingScreening(
                1L,
                screening.getStartTime(),
                screening.getEndTime()
        )).thenReturn(false);

        when(screeningRepository.save(screening))
                .thenReturn(screening);

        Screening result = screeningService.createScreening(screening);

        assertSame(screening, result);
        verify(screeningRepository).save(screening);
    }

    @Test
    void updateScreening_throwsExceptionWhenOverlapping() {
        Room room = new Room();
        room.setId(1L);

        Movie movie = new Movie();
        movie.setDurationInMinutes(120);

        Screening screening = new Screening();
        screening.setId(1L);
        screening.setRoom(room);
        screening.setMovie(movie);
        screening.setStartTime(
                LocalDateTime.of(2026, 8, 17, 20, 0)
        );

        when(screeningRepository.findById(1L))
                .thenReturn(Optional.of(screening));

        when(screeningRepository.existsOverlappingScreeningExcept(
                1L,
                screening.getStartTime(),
                screening.getEndTime(),
                1L
        )).thenReturn(true);

        assertThrows(
                ScreeningOverlapException.class,
                () -> screeningService.updateScreening(screening)
        );
    }

    @Test
    void updateScreening_throwsExceptionWhenNotFound() {
        Screening screening = new Screening();
        screening.setId(1L);

        when(screeningRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> screeningService.updateScreening(screening)
        );
    }

    @Test
    void updateScreening_updatesFields() {
        Room oldRoom = new Room();
        oldRoom.setId(1L);

        Room newRoom = new Room();
        newRoom.setId(2L);

        Movie oldMovie = new Movie();
        oldMovie.setDurationInMinutes(120);

        Movie newMovie = new Movie();
        newMovie.setDurationInMinutes(150);

        Screening existingScreening = new Screening();
        existingScreening.setId(1L);
        existingScreening.setRoom(oldRoom);
        existingScreening.setMovie(oldMovie);
        existingScreening.setStartTime(
                LocalDateTime.of(2026, 8, 17, 18, 0)
        );
        existingScreening.setPriceInCents(1000);

        Screening updatedScreening = new Screening();
        updatedScreening.setId(1L);
        updatedScreening.setRoom(newRoom);
        updatedScreening.setMovie(newMovie);
        updatedScreening.setStartTime(
                LocalDateTime.of(2026, 8, 17, 20, 0)
        );
        updatedScreening.setPriceInCents(1500);

        when(screeningRepository.existsOverlappingScreeningExcept(
                2L,
                updatedScreening.getStartTime(),
                updatedScreening.getEndTime(),
                1L
        )).thenReturn(false);

        when(screeningRepository.findById(1L))
                .thenReturn(Optional.of(existingScreening));

        Screening result = screeningService.updateScreening(updatedScreening);

        assertSame(existingScreening, result);
        assertEquals(newRoom, result.getRoom());
        assertEquals(newMovie, result.getMovie());
        assertEquals(
                updatedScreening.getStartTime(),
                result.getStartTime()
        );
        assertEquals(1500, result.getPriceInCents());
    }

    @Test
    void deleteScreening_deletesScreeningById() {
        screeningService.deleteScreening(1L);

        verify(screeningRepository).deleteById(1L);
    }
}