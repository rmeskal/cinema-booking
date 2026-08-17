package com.rayan.cinemaapi.service;

import com.rayan.cinemaapi.entity.Screening;
import com.rayan.cinemaapi.exception.EntityNotFoundException;
import com.rayan.cinemaapi.exception.ScreeningOverlapException;
import com.rayan.cinemaapi.repository.ScreeningRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScreeningService {

    private final ScreeningRepository screeningRepository;

    public ScreeningService(ScreeningRepository screeningRepository) {
        this.screeningRepository = screeningRepository;
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
        // A room cannot have two screenings that overlap in time, excluding the current screening
        if (screeningRepository.existsOverlappingScreeningExcept(
                screening.getRoom().getId(),
                screening.getStartTime(),
                screening.getEndTime(),
                screening.getId()
        )) {
            throw new ScreeningOverlapException(screening.getRoom().getId());
        }

        Screening existingScreening = getScreening(screening.getId());

        existingScreening.setStartTime(screening.getStartTime());
        existingScreening.setPriceInCents(screening.getPriceInCents());
        existingScreening.setMovie(screening.getMovie());
        existingScreening.setRoom(screening.getRoom());

        return existingScreening;
    }

    public void deleteScreening(Long id) {
        screeningRepository.deleteById(id);
    }
}
