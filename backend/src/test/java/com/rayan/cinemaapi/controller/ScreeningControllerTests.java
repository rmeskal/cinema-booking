package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.TestDataFactory;
import com.rayan.cinemaapi.entity.Movie;
import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.RoomType;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.repository.MovieRepository;
import com.rayan.cinemaapi.repository.RoomRepository;
import com.rayan.cinemaapi.repository.RoomTypeRepository;
import com.rayan.cinemaapi.repository.ScreeningRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ScreeningControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ScreeningRepository screeningRepository;

    private Movie saveMovie() {
        return movieRepository.save(TestDataFactory.createMovie());
    }

    private Room saveRoom() {
        RoomType roomType = roomTypeRepository.save(
                TestDataFactory.createRoomType()
        );

        return roomRepository.save(
                TestDataFactory.createRoom(roomType)
        );
    }

    private Screening saveScreening(
            Movie movie,
            Room room,
            LocalDateTime startTime
    ) {
        return screeningRepository.save(
                TestDataFactory.createScreening(
                        movie,
                        room,
                        startTime
                )
        );
    }

    @Test
    void getScreenings_returnsScreenings() throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        mockMvc.perform(get("/api/screenings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(screening.getId()))
                .andExpect(jsonPath("$[0].movieId").value(movie.getId()))
                .andExpect(jsonPath("$[0].roomId").value(room.getId()))
                .andExpect(jsonPath("$[0].startTime")
                        .value("2026-08-25T19:00:00"))
                .andExpect(jsonPath("$[0].priceInCents").value(1000));
    }

    @Test
    void getScreening_returnsScreeningWhenFound() throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        mockMvc.perform(get("/api/screenings/{id}", screening.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(screening.getId()))
                .andExpect(jsonPath("$.movieId").value(movie.getId()))
                .andExpect(jsonPath("$.roomId").value(room.getId()))
                .andExpect(jsonPath("$.startTime")
                        .value("2026-08-25T19:00:00"))
                .andExpect(jsonPath("$.priceInCents").value(1000));
    }

    @Test
    void getScreening_returns404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/screenings/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createScreening_createsScreening() throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        mockMvc.perform(post("/api/screenings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "startTime": "2026-08-25T19:00:00",
                                    "priceInCents": 1200,
                                    "movieId": %d,
                                    "roomId": %d
                                }
                                """.formatted(
                                movie.getId(),
                                room.getId()
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movieId").value(movie.getId()))
                .andExpect(jsonPath("$.roomId").value(room.getId()))
                .andExpect(jsonPath("$.startTime")
                        .value("2026-08-25T19:00:00"))
                .andExpect(jsonPath("$.priceInCents").value(1200));

        assertEquals(
                1,
                screeningRepository.count()
        );
    }

    @Test
    void createScreening_returns404WhenMovieDoesNotExist()
            throws Exception {
        Room room = saveRoom();

        mockMvc.perform(post("/api/screenings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "startTime": "2026-08-25T19:00:00",
                                    "priceInCents": 1200,
                                    "movieId": 999999,
                                    "roomId": %d
                                }
                                """.formatted(room.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void createScreening_returns404WhenRoomDoesNotExist()
            throws Exception {
        Movie movie = saveMovie();

        mockMvc.perform(post("/api/screenings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "startTime": "2026-08-25T19:00:00",
                                    "priceInCents": 1200,
                                    "movieId": %d,
                                    "roomId": 999999
                                }
                                """.formatted(movie.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void createScreening_returns409WhenScreeningsOverlap()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        mockMvc.perform(post("/api/screenings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "startTime": "2026-08-25T20:00:00",
                                    "priceInCents": 1200,
                                    "movieId": %d,
                                    "roomId": %d
                                }
                                """.formatted(
                                movie.getId(),
                                room.getId()
                        )))
                .andExpect(status().isConflict());
    }

    @Test
    void createScreening_allowsNonOverlappingScreenings()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        mockMvc.perform(post("/api/screenings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "startTime": "2026-08-25T22:00:00",
                                    "priceInCents": 1200,
                                    "movieId": %d,
                                    "roomId": %d
                                }
                                """.formatted(
                                movie.getId(),
                                room.getId()
                        )))
                .andExpect(status().isCreated());

        assertEquals(2, screeningRepository.count());
    }

    @Test
    void createScreening_returns400WhenRequestIsInvalid()
            throws Exception {
        mockMvc.perform(post("/api/screenings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "startTime": null,
                                    "priceInCents": 1200,
                                    "movieId": null,
                                    "roomId": null
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateScreening_updatesScreening() throws Exception {
        Movie oldMovie = saveMovie();
        Movie newMovie = movieRepository.save(
                TestDataFactory.createMovie(
                        "New Movie",
                        "New description",
                        90
                )
        );

        Room oldRoom = saveRoom();

        RoomType newRoomType = roomTypeRepository.save(
                TestDataFactory.createRoomType("Premium")
        );

        Room newRoom = roomRepository.save(
                TestDataFactory.createRoom(
                        "New Room",
                        newRoomType
                )
        );

        Screening screening = saveScreening(
                oldMovie,
                oldRoom,
                LocalDateTime.of(2026, 8, 25, 18, 0)
        );

        mockMvc.perform(put("/api/screenings/{id}", screening.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "startTime": "2026-08-25T20:00:00",
                                    "priceInCents": 1500,
                                    "movieId": %d,
                                    "roomId": %d
                                }
                                """.formatted(
                                newMovie.getId(),
                                newRoom.getId()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(screening.getId()))
                .andExpect(jsonPath("$.movieId").value(newMovie.getId()))
                .andExpect(jsonPath("$.roomId").value(newRoom.getId()))
                .andExpect(jsonPath("$.startTime")
                        .value("2026-08-25T20:00:00"))
                .andExpect(jsonPath("$.priceInCents").value(1500));

        Screening updated = screeningRepository
                .findById(screening.getId())
                .orElseThrow();

        assertEquals(newMovie.getId(), updated.getMovie().getId());
        assertEquals(newRoom.getId(), updated.getRoom().getId());
        assertEquals(
                LocalDateTime.of(2026, 8, 25, 20, 0),
                updated.getStartTime()
        );
        assertEquals(1500, updated.getPriceInCents());
    }

    @Test
    void updateScreening_returns404WhenScreeningDoesNotExist()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        mockMvc.perform(put("/api/screenings/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "startTime": "2026-08-25T20:00:00",
                                    "priceInCents": 1500,
                                    "movieId": %d,
                                    "roomId": %d
                                }
                                """.formatted(
                                movie.getId(),
                                room.getId()
                        )))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateScreening_returns404WhenMovieDoesNotExist()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 18, 0)
        );

        mockMvc.perform(put("/api/screenings/{id}", screening.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "startTime": "2026-08-25T20:00:00",
                                    "priceInCents": 1500,
                                    "movieId": 999999,
                                    "roomId": %d
                                }
                                """.formatted(room.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateScreening_returns404WhenRoomDoesNotExist()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 18, 0)
        );

        mockMvc.perform(put("/api/screenings/{id}", screening.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "startTime": "2026-08-25T20:00:00",
                                    "priceInCents": 1500,
                                    "movieId": %d,
                                    "roomId": 999999
                                }
                                """.formatted(movie.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateScreening_returns409WhenScreeningsOverlap()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        Screening screeningToUpdate = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 22, 0)
        );

        mockMvc.perform(
                        put(
                                "/api/screenings/{id}",
                                screeningToUpdate.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "startTime": "2026-08-25T20:00:00",
                                            "priceInCents": 1500,
                                            "movieId": %d,
                                            "roomId": %d
                                        }
                                        """.formatted(
                                        movie.getId(),
                                        room.getId()
                                ))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void updateScreening_allowsNonOverlappingTime()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        Screening screeningToUpdate = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 22, 0)
        );

        mockMvc.perform(
                        put(
                                "/api/screenings/{id}",
                                screeningToUpdate.getId()
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "startTime": "2026-08-25T21:00:00",
                                            "priceInCents": 1500,
                                            "movieId": %d,
                                            "roomId": %d
                                        }
                                        """.formatted(
                                        movie.getId(),
                                        room.getId()
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(screeningToUpdate.getId()))
                .andExpect(jsonPath("$.startTime")
                        .value("2026-08-25T21:00:00"));
    }

    @Test
    void deleteScreening_deletesScreening()
            throws Exception {
        Movie movie = saveMovie();
        Room room = saveRoom();

        Screening screening = saveScreening(
                movie,
                room,
                LocalDateTime.of(2026, 8, 25, 19, 0)
        );

        mockMvc.perform(
                        delete("/api/screenings/{id}", screening.getId())
                )
                .andExpect(status().isNoContent());

        assertTrue(
                screeningRepository.findById(screening.getId()).isEmpty()
        );
    }

    @Test
    void deleteScreening_returns404WhenNotFound()
            throws Exception {
        mockMvc.perform(delete("/api/screenings/999999"))
                .andExpect(status().isNotFound());
    }
}