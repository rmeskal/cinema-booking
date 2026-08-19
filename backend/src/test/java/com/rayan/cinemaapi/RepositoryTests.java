package com.rayan.cinemaapi;

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
        Movie movie = new Movie();
        movie.setTitle("Test Movie");
        movie.setDescription("A test movie");
        movie.setDurationInMinutes(120);

        Movie saved = movieRepository.save(movie);

        assertNotNull(saved.getId());

        Movie found = movieRepository.findById(saved.getId()).orElseThrow();

        assertEquals("Test Movie", found.getTitle());
        assertEquals("A test movie", found.getDescription());
        assertEquals(120, found.getDurationInMinutes());
    }

    @Test
    void findAllMovies() {
        Movie first = new Movie();
        first.setTitle("First movie");
        first.setDescription("The first movie");
        first.setDurationInMinutes(100);

        Movie second = new Movie();
        second.setTitle("Second movie");
        second.setDescription("The second movie");
        second.setDurationInMinutes(120);

        movieRepository.save(first);
        movieRepository.save(second);

        var movies = movieRepository.findAll();

        assertEquals(2, movies.size());
        assertTrue(movies.contains(first));
        assertTrue(movies.contains(second));
    }

    @Test
    void existsById() {
        Movie movie = new Movie();
        movie.setTitle("Test movie");
        movie.setDescription("A test movie");
        movie.setDurationInMinutes(120);

        Movie saved = movieRepository.save(movie);

        assertTrue(movieRepository.existsById(saved.getId()));
        assertFalse(movieRepository.existsById(999999L));
    }

    @Test
    void deleteById() {
        Movie movie = new Movie();
        movie.setTitle("Test movie");
        movie.setDescription("A test movie");
        movie.setDurationInMinutes(120);

        Movie saved = movieRepository.save(movie);

        assertTrue(movieRepository.existsById(saved.getId()));

        movieRepository.deleteById(saved.getId());

        assertFalse(movieRepository.existsById(saved.getId()));
    }

    @Test
    void saveAndFindScreeningWithRelationships() {
        Movie movie = new Movie();
        movie.setTitle("Test movie");
        movie.setDescription("A test movie");
        movie.setDurationInMinutes(120);
        movie = movieRepository.save(movie);

        RoomType roomType = new RoomType();
        roomType.setName("Test room type");
        roomType = roomTypeRepository.save(roomType);

        Room room = new Room();
        room.setRoomType(roomType);
        room = roomRepository.save(room);

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
        RoomType roomType = new RoomType();
        roomType.setName("Test room type");
        roomType = roomTypeRepository.save(roomType);

        Room room = new Room();
        room.setRoomType(roomType);
        room = roomRepository.save(room);

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