package com.rayan.cinemaapi.dto.screening;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record ScreeningRequest(
        @NotNull LocalDateTime startTime,
        @NotNull @PositiveOrZero Integer priceInCents,
        @NotNull Long movieId,
        @NotNull Long roomId
) {
}
