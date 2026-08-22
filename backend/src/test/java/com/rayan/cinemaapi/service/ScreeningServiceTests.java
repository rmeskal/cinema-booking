package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.TestDataFactory;
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

    @Mock
    private MovieService movieService;

    @Mock
    private RoomService roomService;

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
    void createScreening_throwsExceptionWhenMovieNotFound() {
        Movie movie = TestDataFactory.createMovie();
        movie.setId(1L);

        Room room = TestDataFactory.createRoom(
                TestDataFactory.createRoomType()
        );
        room.setId(1L);

        Screening screening = TestDataFactory.createScreening(movie, room);

        when(movieService.getMovie(1L))
                .thenThrow(new EntityNotFoundException("Movie", 1L));

        assertThrows(
                EntityNotFoundException.class,
                () -> screeningService.createScreening(screening)
        );

        verifyNoInteractions(screeningRepository);
    }

    @Test
    void createScreening_throwsExceptionWhenRoomNotFound() {
        Movie movie = TestDataFactory.createMovie();
        movie.setId(1L);

        Room room = TestDataFactory.createRoom(
                TestDataFactory.createRoomType()
        );
        room.setId(1L);

        Screening screening = TestDataFactory.createScreening(movie, room);

        when(movieService.getMovie(1L))
                .thenReturn(movie);

        when(roomService.getRoom(1L))
                .thenThrow(new EntityNotFoundException("Room", 1L));

        assertThrows(
                EntityNotFoundException.class,
                () -> screeningService.createScreening(screening)
        );

        verifyNoInteractions(screeningRepository);
    }

    @Test
    void createScreening_throwsExceptionWhenOverlapping() {
        Movie movie = TestDataFactory.createMovie();
        movie.setId(1L);

        Room room = TestDataFactory.createRoom(
                TestDataFactory.createRoomType()
        );
        room.setId(1L);

        Screening screening = TestDataFactory.createScreening(movie, room);

        when(movieService.getMovie(1L))
                .thenReturn(movie);

        when(roomService.getRoom(1L))
                .thenReturn(room);

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
        Movie movie = TestDataFactory.createMovie();
        movie.setId(1L);

        Room room = TestDataFactory.createRoom(
                TestDataFactory.createRoomType()
        );
        room.setId(1L);

        Screening screening = TestDataFactory.createScreening(movie, room);

        when(movieService.getMovie(1L))
                .thenReturn(movie);

        when(roomService.getRoom(1L))
                .thenReturn(room);

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
    void updateScreening_throwsExceptionWhenMovieNotFound() {
        Movie movie = TestDataFactory.createMovie();
        movie.setId(2L);

        Room room = TestDataFactory.createRoom(
                TestDataFactory.createRoomType()
        );
        room.setId(2L);

        Screening existingScreening =
                TestDataFactory.createScreening(movie, room);
        existingScreening.setId(1L);

        Screening updatedScreening =
                TestDataFactory.createScreening(movie, room);
        updatedScreening.setId(1L);

        when(screeningRepository.findById(1L))
                .thenReturn(Optional.of(existingScreening));

        when(movieService.getMovie(2L))
                .thenThrow(new EntityNotFoundException("Movie", 2L));

        assertThrows(
                EntityNotFoundException.class,
                () -> screeningService.updateScreening(updatedScreening)
        );

        verify(screeningRepository, never())
                .existsOverlappingScreeningExcept(anyLong(), any(), any(), anyLong());
    }

    @Test
    void updateScreening_throwsExceptionWhenRoomNotFound() {
        Movie movie = TestDataFactory.createMovie();
        movie.setId(2L);

        Room room = TestDataFactory.createRoom(
                TestDataFactory.createRoomType()
        );
        room.setId(2L);

        Screening existingScreening =
                TestDataFactory.createScreening(movie, room);
        existingScreening.setId(1L);

        Screening updatedScreening =
                TestDataFactory.createScreening(movie, room);
        updatedScreening.setId(1L);

        when(screeningRepository.findById(1L))
                .thenReturn(Optional.of(existingScreening));

        when(movieService.getMovie(2L))
                .thenReturn(movie);

        when(roomService.getRoom(2L))
                .thenThrow(new EntityNotFoundException("Room", 2L));

        assertThrows(
                EntityNotFoundException.class,
                () -> screeningService.updateScreening(updatedScreening)
        );

        verify(screeningRepository, never())
                .existsOverlappingScreeningExcept(anyLong(), any(), any(), anyLong());
    }

    @Test
    void updateScreening_throwsExceptionWhenOverlapping() {
        Movie movie = TestDataFactory.createMovie();
        movie.setId(1L);

        Room room = TestDataFactory.createRoom(
                TestDataFactory.createRoomType()
        );
        room.setId(1L);

        Screening screening = TestDataFactory.createScreening(movie, room);
        screening.setId(1L);

        LocalDateTime newStartTime = screening.getStartTime();
        LocalDateTime newEndTime = newStartTime
                .plusMinutes(movie.getDurationInMinutes());

        when(screeningRepository.findById(1L))
                .thenReturn(Optional.of(screening));

        when(movieService.getMovie(1L))
                .thenReturn(movie);

        when(roomService.getRoom(1L))
                .thenReturn(room);

        when(screeningRepository.existsOverlappingScreeningExcept(
                room.getId(),
                newStartTime,
                newEndTime,
                screening.getId()
        )).thenReturn(true);

        assertThrows(
                ScreeningOverlapException.class,
                () -> screeningService.updateScreening(screening)
        );
    }

    @Test
    void updateScreening_updatesFields() {
        Movie oldMovie = TestDataFactory.createMovie();
        oldMovie.setId(1L);

        Room oldRoom = TestDataFactory.createRoom(
                TestDataFactory.createRoomType()
        );
        oldRoom.setId(1L);

        Movie newMovie = TestDataFactory.createMovie();
        newMovie.setId(2L);
        newMovie.setDurationInMinutes(150);

        Room newRoom = TestDataFactory.createRoom(
                TestDataFactory.createRoomType()
        );
        newRoom.setId(2L);

        Screening existingScreening =
                TestDataFactory.createScreening(oldMovie, oldRoom);
        existingScreening.setId(1L);
        existingScreening.setStartTime(
                LocalDateTime.of(2026, 8, 17, 18, 0)
        );
        existingScreening.setPriceInCents(1000);

        LocalDateTime newStartTime =
                LocalDateTime.of(2026, 8, 17, 20, 0);

        LocalDateTime newEndTime =
                newStartTime.plusMinutes(newMovie.getDurationInMinutes());

        Screening updatedScreening =
                TestDataFactory.createScreening(
                        newMovie,
                        newRoom,
                        newStartTime
                );
        updatedScreening.setId(1L);
        updatedScreening.setPriceInCents(1500);

        when(screeningRepository.findById(1L))
                .thenReturn(Optional.of(existingScreening));

        when(movieService.getMovie(2L))
                .thenReturn(newMovie);

        when(roomService.getRoom(2L))
                .thenReturn(newRoom);

        when(screeningRepository.existsOverlappingScreeningExcept(
                newRoom.getId(),
                newStartTime,
                newEndTime,
                updatedScreening.getId()
        )).thenReturn(false);

        Screening result =
                screeningService.updateScreening(updatedScreening);

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
        when(screeningRepository.existsById(1L))
                .thenReturn(true);

        screeningService.deleteScreening(1L);

        verify(screeningRepository).deleteById(1L);
    }
}