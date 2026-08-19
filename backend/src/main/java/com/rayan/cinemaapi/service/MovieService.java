package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Movie;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
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
