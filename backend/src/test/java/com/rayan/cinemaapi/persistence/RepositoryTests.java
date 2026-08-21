package com.rayan.cinemaapi.persistence;

import com.rayan.cinemaapi.TestDataFactory;
import com.rayan.cinemaapi.entity.*;
import com.rayan.cinemaapi.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RepositoryTests {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private ScreeningRepository screeningRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Test
    void saveAndFindMovie() {
        Movie movie = TestDataFactory.createMovie(
                "Test Movie",
                "A test movie",
                120
        );

        Movie saved = movieRepository.save(movie);

        assertNotNull(saved.getId());

        Movie found = movieRepository.findById(saved.getId()).orElseThrow();

        assertEquals("Test Movie", found.getTitle());
        assertEquals("A test movie", found.getDescription());
        assertEquals(120, found.getDurationInMinutes());
    }

    @Test
    void findAllMovies() {
        Movie first = TestDataFactory.createMovie(
                "First movie",
                "The first movie",
                100
        );

        Movie second = TestDataFactory.createMovie(
                "Second movie",
                "The second movie",
                120
        );

        movieRepository.save(first);
        movieRepository.save(second);

        var movies = movieRepository.findAll();

        assertEquals(2, movies.size());
        assertTrue(movies.contains(first));
        assertTrue(movies.contains(second));
    }

    @Test
    void existsById() {
        Movie movie = TestDataFactory.createMovie();

        Movie saved = movieRepository.save(movie);

        assertTrue(movieRepository.existsById(saved.getId()));
        assertFalse(movieRepository.existsById(999999L));
    }

    @Test
    void deleteById() {
        Movie movie = TestDataFactory.createMovie();

        Movie saved = movieRepository.save(movie);

        assertTrue(movieRepository.existsById(saved.getId()));

        movieRepository.deleteById(saved.getId());

        assertFalse(movieRepository.existsById(saved.getId()));
    }

    @Test
    void saveAndFindScreeningWithRelationships() {
        Movie movie = movieRepository.save(TestDataFactory.createMovie());

        RoomType roomType = roomTypeRepository.save(
                TestDataFactory.createRoomType("Test room type")
        );

        Room room = roomRepository.save(
                TestDataFactory.createRoom(roomType)
        );

        Screening screening = new Screening();
        screening.setMovie(movie);
        screening.setRoom(room);
        screening.setStartTime(LocalDateTime.of(2026, 8, 20, 20, 0));
        screening.setPriceInCents(1200);

        Screening saved = screeningRepository.save(screening);

        Screening found = screeningRepository.findById(saved.getId()).orElseThrow();

        assertEquals(movie.getId(), found.getMovie().getId());
        assertEquals(room.getId(), found.getRoom().getId());
    }

    @Test
    void saveAndFindSeatWithCompositeId() {
        RoomType roomType = roomTypeRepository.save(
                TestDataFactory.createRoomType("Test room type")
        );

        Room room = roomRepository.save(
                TestDataFactory.createRoom(roomType)
        );

        SeatId seatId = new SeatId();
        seatId.setSeatLabel("A1");

        Seat seat = new Seat();
        seat.setSeatId(seatId);
        seat.setRoom(room);

        Seat saved = seatRepository.save(seat);

        assertNotNull(saved.getSeatId());
        assertEquals("A1", saved.getSeatId().getSeatLabel());
        assertEquals(room.getId(), saved.getSeatId().getRoomId());

        Seat found = seatRepository.findById(saved.getSeatId()).orElseThrow();

        assertEquals("A1", found.getSeatId().getSeatLabel());
        assertEquals(room.getId(), found.getSeatId().getRoomId());
    }
}