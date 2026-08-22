package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.dto.screening.ScreeningRequest;
import com.rayan.cinemaapi.dto.screening.ScreeningResponse;
import com.rayan.cinemaapi.entity.Movie;
import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.service.ScreeningService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screenings")
public class ScreeningController {

    private final ScreeningService screeningService;

    public ScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    @GetMapping
    public List<ScreeningResponse> getScreenings() {
        return screeningService.getScreenings()
                .stream()
                .map(this::toResponse)
                .toList();
    }
    
    @GetMapping("/{id}")
    public ScreeningResponse getScreening(@PathVariable Long id) {
        return toResponse(screeningService.getScreening(id));
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScreeningResponse createScreening(@Valid @RequestBody ScreeningRequest request) {
        return toResponse(screeningService.createScreening(toEntity(request)));
    }

    @PutMapping("/{id}")
    public ScreeningResponse updateScreening(
            @PathVariable Long id,
            @Valid @RequestBody ScreeningRequest request
    ) {
        Screening screening = toEntity(request);
        screening.setId(id);

        return toResponse(screeningService.updateScreening(screening));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable Long id) {
        screeningService.deleteScreening(id);
    }
    

    private Screening toEntity(ScreeningRequest request) {
        Screening screening = new Screening();
        screening.setStartTime(request.startTime());
        screening.setPriceInCents(request.priceInCents());

        Movie movie = new Movie();
        movie.setId(request.movieId());
        screening.setMovie(movie);

        Room room = new Room();
        room.setId(request.roomId());
        screening.setRoom(room);

        return screening;
    }

    private ScreeningResponse toResponse(Screening screening) {
        return new ScreeningResponse(
                screening.getId(),
                screening.getStartTime(),
                screening.getPriceInCents(),
                screening.getMovie().getId(),
                screening.getRoom().getId()
        );
    }
}
