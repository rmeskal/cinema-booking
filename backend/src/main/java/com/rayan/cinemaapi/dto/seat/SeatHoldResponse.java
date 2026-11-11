package com.rayan.cinemaapi.dto.seat;

import java.time.Instant;

public record SeatHoldResponse(
        Long screeningId,
        String seatLabel,
        Long roomId,
        Long userId,
        Instant expiresAt
) {
}
