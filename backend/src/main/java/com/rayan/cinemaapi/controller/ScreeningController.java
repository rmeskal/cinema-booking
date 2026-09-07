package com.rayan.cinemaapi.controller;

import com.rayan.cinemaapi.dto.screening.ScreeningRequest;
import com.rayan.cinemaapi.dto.screening.ScreeningResponse;
import com.rayan.cinemaapi.dto.seat.SeatAvailabilityResponse;
import com.rayan.cinemaapi.entity.Movie;
import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.service.ScreeningService;
import com.rayan.cinemaapi.service.SeatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screenings")
@Tag(name = "Screenings", description = "Manage screenings")
public class ScreeningController {

    private final ScreeningService screeningService;
    private final SeatService seatService;

    public ScreeningController(ScreeningService screeningService, SeatService seatService) {
        this.screeningService = screeningService;
        this.seatService = seatService;
    }

    @GetMapping
    @Operation(summary = "Get all screenings")
    public List<ScreeningResponse> getScreenings() {
        return screeningService.getScreenings()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a screening by ID")
    public ScreeningResponse getScreening(@PathVariable Long id) {
        return toResponse(screeningService.getScreening(id));
    }

    @PostMapping
    @Operation(summary = "Create a screening")
    @ResponseStatus(HttpStatus.CREATED)
    public ScreeningResponse createScreening(@Valid @RequestBody ScreeningRequest request) {
        return toResponse(screeningService.createScreening(toEntity(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a screening")
    public ScreeningResponse updateScreening(
            @PathVariable Long id,
            @Valid @RequestBody ScreeningRequest request
    ) {
        Screening screening = toEntity(request);
        screening.setId(id);

        return toResponse(screeningService.updateScreening(screening));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a screening")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteScreening(@PathVariable Long id) {
        screeningService.deleteScreening(id);
    }

    @GetMapping("/{id}/seats")
    @Operation(summary = "Get seat availability for a screening")
    public List<SeatAvailabilityResponse> getSeatAvailability(
            @PathVariable Long id
    ) {
        return seatService.getSeatAvailability(id);
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