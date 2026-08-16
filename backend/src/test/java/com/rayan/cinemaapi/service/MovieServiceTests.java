package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Movie;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceTests {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    @Test
    void getMovie_returnsMovieWhenFound() {
        Movie movie = new Movie();
        movie.setId(1L);

        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));

        Movie result = movieService.getMovie(1L);

        assertSame(movie, result);
    }

    @Test
    void getMovie_throwsExceptionWhenNotFound() {
        when(movieRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> movieService.getMovie(1L)
        );
    }

    @Test
    void updateMovie_updatesFields() {
        Movie existingMovie = new Movie();
        existingMovie.setId(1L);
        existingMovie.setTitle("Old title");
        existingMovie.setDescription("Old description");
        existingMovie.setDurationInMinutes(100);
        existingMovie.setThumbnailUrl("old.jpg");

        Movie updatedMovie = new Movie();
        updatedMovie.setId(1L);
        updatedMovie.setTitle("New title");
        updatedMovie.setDescription("New description");
        updatedMovie.setDurationInMinutes(120);
        updatedMovie.setThumbnailUrl("new.jpg");

        when(movieRepository.findById(1L)).thenReturn(Optional.of(existingMovie));

        Movie result = movieService.updateMovie(updatedMovie);

        assertEquals("New title", result.getTitle());
        assertEquals("New description", result.getDescription());
        assertEquals(120, result.getDurationInMinutes());
        assertEquals("new.jpg", result.getThumbnailUrl());
    }
}