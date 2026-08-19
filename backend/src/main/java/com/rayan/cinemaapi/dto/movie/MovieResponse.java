package com.rayan.cinemaapi.dto.movie;

public record MovieResponse(
        Long id,
        String title,
        Integer durationInMinutes,
        String description,
        String thumbnailUrl
) {
}
