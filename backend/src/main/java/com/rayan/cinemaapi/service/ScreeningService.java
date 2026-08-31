package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Movie;
import com.rayan.cinemaapi.entity.Room;
import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.exception.EntityInUseException;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.exception.ScreeningOverlapException;
import com.rayan.cinemaapi.repository.BookingRepository;
import com.rayan.cinemaapi.repository.ScreeningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScreeningService {

    private final ScreeningRepository screeningRepository;
    private final BookingRepository bookingRepository;
    private final MovieService movieService;
    private final RoomService roomService;

    public ScreeningService(ScreeningRepository screeningRepository, BookingRepository bookingRepository, MovieService movieService, RoomService roomService) {
        this.screeningRepository = screeningRepository;
        this.bookingRepository = bookingRepository;
        this.movieService = movieService;
        this.roomService = roomService;
    }

    public List<Screening> getScreenings() {
        return screeningRepository.findAll();
    }

    public Screening getScreening(Long id) {
        return screeningRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Screening", id)
        );
    }

    public Screening createScreening(Screening screening) {
        Movie movie = movieService.getMovie(screening.getMovie().getId());
        Room room = roomService.getRoom(screening.getRoom().getId());

        screening.setMovie(movie);
        screening.setRoom(room);

        // A room cannot have two screenings that overlap in time
        if (screeningRepository.existsOverlappingScreening(
                screening.getRoom().getId(),
                screening.getStartTime(),
                screening.getEndTime())) {

            throw new ScreeningOverlapException(screening.getRoom().getId());
        }

        return screeningRepository.save(screening);
    }

    @Transactional
    public Screening updateScreening(Screening screening) {
        Screening existingScreening = getScreening(screening.getId());

        Movie movie = movieService.getMovie(screening.getMovie().getId());
        Room room = roomService.getRoom(screening.getRoom().getId());

        LocalDateTime newEndTime =
                screening.getStartTime().plusMinutes(movie.getDurationInMinutes());

        if (screeningRepository.existsOverlappingScreeningExcept(
                room.getId(),
                screening.getStartTime(),
                newEndTime,
                screening.getId()
        )) {
            throw new ScreeningOverlapException(room.getId());
        }

        existingScreening.setMovie(movie);
        existingScreening.setRoom(room);
        existingScreening.setStartTime(screening.getStartTime());
        existingScreening.setPriceInCents(screening.getPriceInCents());

        return existingScreening;
    }

    public void deleteScreening(Long id) {
        if (!screeningRepository.existsById(id)) {
            throw new EntityNotFoundException("Screening", id);
        }

        if (bookingRepository.existsByScreeningId(id)) {
            throw new EntityInUseException("Screening", id);
        }

        screeningRepository.deleteById(id);
    }
}
