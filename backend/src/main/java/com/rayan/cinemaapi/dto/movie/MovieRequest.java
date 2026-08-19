package com.rayan.cinemaapi.dto.movie;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MovieRequest(
        @NotBlank String title,
        @NotNull Integer durationInMinutes,
        String description,
        String thumbnailUrl
) {
}
