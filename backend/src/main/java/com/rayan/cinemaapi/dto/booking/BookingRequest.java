package com.rayan.cinemaapi.dto.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull Long screeningId,
        @NotNull Long userId,
        @NotBlank String seatLabel,
        @NotNull Long roomId
) {
}