package com.rayan.cinemaapi.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomRequest(
        @NotBlank String name,
        @NotNull Long roomTypeId
) {
}
