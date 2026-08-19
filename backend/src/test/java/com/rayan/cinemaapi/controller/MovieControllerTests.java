package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.dto.movie.MovieRequest;
import com.rayan.cinemaapi.entity.Movie;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovieController.class)
class MovieControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private MovieService movieService;

    @Test
    void getMovies_returnsMovies() throws Exception {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Interstellar");
        movie.setDurationInMinutes(169);
        movie.setDescription("A group of explorers travel through a wormhole.");
        movie.setThumbnailUrl("https://example.com/interstellar.jpg");

        when(movieService.getMovies())
                .thenReturn(List.of(movie));

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Interstellar"))
                .andExpect(jsonPath("$[0].durationInMinutes").value(169))
                .andExpect(jsonPath("$[0].description")
                        .value("A group of explorers travel through a wormhole."))
                .andExpect(jsonPath("$[0].thumbnailUrl")
                        .value("https://example.com/interstellar.jpg"));
    }

    @Test
    void getMovies_returnsEmptyListWhenNoMoviesExist() throws Exception {
        when(movieService.getMovies())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getMovie_returnsMovieWhenFound() throws Exception {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Interstellar");
        movie.setDurationInMinutes(169);
        movie.setDescription("A group of explorers travel through a wormhole.");
        movie.setThumbnailUrl("https://example.com/interstellar.jpg");

        when(movieService.getMovie(1L))
                .thenReturn(movie);

        mockMvc.perform(get("/api/movies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Interstellar"))
                .andExpect(jsonPath("$.durationInMinutes").value(169))
                .andExpect(jsonPath("$.description")
                        .value("A group of explorers travel through a wormhole."))
                .andExpect(jsonPath("$.thumbnailUrl")
                        .value("https://example.com/interstellar.jpg"));
    }

    @Test
    void getMovie_returnsNotFoundWhenMovieDoesNotExist() throws Exception {
        when(movieService.getMovie(999L))
                .thenThrow(new EntityNotFoundException("Movie", 999L));

        mockMvc.perform(get("/api/movies/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Movie with id 999 was not found"));
    }

    @Test
    void createMovie_createsMovie() throws Exception {
        MovieRequest request = new MovieRequest(
                "Interstellar",
                169,
                "A group of explorers travel through a wormhole.",
                "https://example.com/interstellar.jpg"
        );

        Movie createdMovie = new Movie();
        createdMovie.setId(1L);
        createdMovie.setTitle("Interstellar");
        createdMovie.setDurationInMinutes(169);
        createdMovie.setDescription("A group of explorers travel through a wormhole.");
        createdMovie.setThumbnailUrl("https://example.com/interstellar.jpg");

        when(movieService.createMovie(any(Movie.class)))
                .thenReturn(createdMovie);

        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Interstellar"))
                .andExpect(jsonPath("$.durationInMinutes").value(169))
                .andExpect(jsonPath("$.description")
                        .value("A group of explorers travel through a wormhole."))
                .andExpect(jsonPath("$.thumbnailUrl")
                        .value("https://example.com/interstellar.jpg"));

        verify(movieService).createMovie(any(Movie.class));
    }

    @Test
    void createMovie_returnsBadRequestWhenTitleIsBlank() throws Exception {
        MovieRequest request = new MovieRequest(
                "",
                169,
                "Description",
                "https://example.com/movie.jpg"
        );

        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(movieService, never()).createMovie(any(Movie.class));
    }

    @Test
    void createMovie_returnsBadRequestWhenDurationIsNull() throws Exception {
        MovieRequest request = new MovieRequest(
                "Interstellar",
                null,
                "Description",
                "https://example.com/movie.jpg"
        );

        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(movieService, never()).createMovie(any(Movie.class));
    }

    @Test
    void createMovie_allowsEmptyDescription() throws Exception {
        MovieRequest request = new MovieRequest(
                "Interstellar",
                169,
                "",
                "https://example.com/movie.jpg"
        );

        Movie createdMovie = new Movie();
        createdMovie.setId(1L);
        createdMovie.setTitle("Interstellar");
        createdMovie.setDurationInMinutes(169);
        createdMovie.setDescription("");
        createdMovie.setThumbnailUrl("https://example.com/movie.jpg");

        when(movieService.createMovie(any(Movie.class)))
                .thenReturn(createdMovie);

        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(movieService).createMovie(any(Movie.class));
    }

    @Test
    void updateMovie_updatesMovie() throws Exception {
        MovieRequest request = new MovieRequest(
                "Updated Interstellar",
                170,
                "Updated description",
                "https://example.com/updated.jpg"
        );

        Movie updatedMovie = new Movie();
        updatedMovie.setId(1L);
        updatedMovie.setTitle("Updated Interstellar");
        updatedMovie.setDurationInMinutes(170);
        updatedMovie.setDescription("Updated description");
        updatedMovie.setThumbnailUrl("https://example.com/updated.jpg");

        when(movieService.updateMovie(any(Movie.class)))
                .thenReturn(updatedMovie);

        mockMvc.perform(put("/api/movies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated Interstellar"))
                .andExpect(jsonPath("$.durationInMinutes").value(170))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.thumbnailUrl")
                        .value("https://example.com/updated.jpg"));

        verify(movieService).updateMovie(any(Movie.class));
    }

    @Test
    void updateMovie_returnsNotFoundWhenMovieDoesNotExist() throws Exception {
        MovieRequest request = new MovieRequest(
                "Updated Interstellar",
                170,
                "Updated description",
                "https://example.com/updated.jpg"
        );

        when(movieService.updateMovie(any(Movie.class)))
                .thenThrow(new EntityNotFoundException("Movie", 999L));

        mockMvc.perform(put("/api/movies/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Movie with id 999 was not found"));
    }

    @Test
    void updateMovie_returnsBadRequestWhenTitleIsBlank() throws Exception {
        MovieRequest request = new MovieRequest(
                "",
                170,
                "Updated description",
                "https://example.com/updated.jpg"
        );

        mockMvc.perform(put("/api/movies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(movieService, never()).updateMovie(any(Movie.class));
    }

    @Test
    void deleteMovie_deletesMovie() throws Exception {
        doNothing().when(movieService).deleteMovie(1L);

        mockMvc.perform(delete("/api/movies/1"))
                .andExpect(status().isNoContent());

        verify(movieService).deleteMovie(1L);
    }

    @Test
    void deleteMovie_returnsNotFoundWhenMovieDoesNotExist() throws Exception {
        doThrow(new EntityNotFoundException("Movie", 999L))
                .when(movieService)
                .deleteMovie(999L);

        mockMvc.perform(delete("/api/movies/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Movie with id 999 was not found"));

        verify(movieService).deleteMovie(999L);
    }
}