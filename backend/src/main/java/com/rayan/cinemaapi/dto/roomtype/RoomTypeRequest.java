package com.rayan.cinemaapi.dto.roomtype;

import jakarta.validation.constraints.NotBlank;

public record RoomTypeRequest(
        @NotBlank String name
) {
}
