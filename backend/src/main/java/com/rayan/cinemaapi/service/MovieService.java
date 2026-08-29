package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Movie;
import com.rayan.cinemaapi.exception.EntityInUseException;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.MovieRepository;
import com.rayan.cinemaapi.repository.ScreeningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;

    public MovieService(MovieRepository movieRepository, ScreeningRepository screeningRepository) {
        this.movieRepository = movieRepository;
        this.screeningRepository = screeningRepository;
    }

    public List<Movie> getMovies() {
        return movieRepository.findAll();
    }

    public Movie getMovie(Long id) {
        return movieRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Movie", id)
        );
    }

    public Movie createMovie(Movie movie) {
        return movieRepository.save(movie);
    }

    public void deleteMovie(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new EntityNotFoundException("Movie", id);
        }

        if (screeningRepository.existsByMovieId(id)) {
            throw new EntityInUseException("Movie", id);
        }

        movieRepository.deleteById(id);
    }

    @Transactional
    public Movie updateMovie(Movie movie) {
        Movie existingMovie = getMovie(movie.getId());

        existingMovie.setTitle(movie.getTitle());
        existingMovie.setDescription(movie.getDescription());
        existingMovie.setDurationInMinutes(movie.getDurationInMinutes());
        existingMovie.setThumbnailUrl(movie.getThumbnailUrl());

        return existingMovie;
    }
}
