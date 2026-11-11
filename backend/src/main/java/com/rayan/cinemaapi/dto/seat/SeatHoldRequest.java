package com.rayan.cinemaapi.dto.seat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SeatHoldRequest(@NotNull @Positive Long userId) {
}
