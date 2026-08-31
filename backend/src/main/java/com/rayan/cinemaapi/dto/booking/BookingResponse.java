package com.rayan.cinemaapi.dto.booking;

public record BookingResponse(
        Long id,
        Long screeningId,
        Long userId,
        String seatLabel,
        Long roomId
) {
}