package com.rayan.cinemaapi.dto.movie;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MovieRequest(
        @NotBlank String title,
        @NotNull @Positive Integer durationInMinutes,
        String description,
        String thumbnailUrl
) {
}
