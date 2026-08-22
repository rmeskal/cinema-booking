package com.rayan.cinemaapi.dto.screening;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ScreeningRequest(
        @NotNull LocalDateTime startTime,
        @NotNull Integer priceInCents,
        @NotNull Long movieId,
        @NotNull Long roomId
) {
}
