package com.rayan.cinemaapi.dto.seat;

import com.rayan.cinemaapi.entity.SeatStatus;

public record SeatAvailabilityResponse(
        String seatLabel,
        Long roomId,
        SeatStatus status
) {}
