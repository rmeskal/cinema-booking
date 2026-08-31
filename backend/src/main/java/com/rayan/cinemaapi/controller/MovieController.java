package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.dto.movie.MovieRequest;
import com.rayan.cinemaapi.dto.movie.MovieResponse;
import com.rayan.cinemaapi.entity.Movie;
import com.rayan.cinemaapi.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@Tag(name = "Movies", description = "Manage movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    @Operation(summary = "Get all movies")
    public List<MovieResponse> getMovies() {
        return movieService.getMovies()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a movie by ID")
    public MovieResponse getMovie(@PathVariable Long id) {
        return toResponse(movieService.getMovie(id));
    }

    @PostMapping
    @Operation(summary = "Create a movie")
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse createMovie(@Valid @RequestBody MovieRequest request) {
        return toResponse(movieService.createMovie(toEntity(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a movie")
    public MovieResponse updateMovie(
            @PathVariable Long id,
            @Valid @RequestBody MovieRequest request
    ) {
        Movie movie = toEntity(request);
        movie.setId(id);

        return toResponse(movieService.updateMovie(movie));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a movie")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
    }

    private Movie toEntity(MovieRequest request) {
        Movie movie = new Movie();
        movie.setTitle(request.title());
        movie.setDescription(request.description());
        movie.setDurationInMinutes(request.durationInMinutes());
        movie.setThumbnailUrl(request.thumbnailUrl());
        return movie;
    }

    private MovieResponse toResponse(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getDurationInMinutes(),
                movie.getDescription(),
                movie.getThumbnailUrl()
        );
    }
}