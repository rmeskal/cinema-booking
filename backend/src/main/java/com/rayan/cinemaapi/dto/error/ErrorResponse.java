package com.rayan.cinemaapi.dto.error;

public record ErrorResponse(
        int status,
        String message
) {
}
