package com.rayan.cinemaapi.dto.screening;

import java.time.LocalDateTime;

public record ScreeningResponse(
        Long id,
        LocalDateTime startTime,
        Integer priceInCents,
        Long movieId,
        Long roomId
) {
}
