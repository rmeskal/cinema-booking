package com.rayan.cinemaapi.exception;

public class ScreeningOverlapException extends RuntimeException {
    public ScreeningOverlapException(Long roomId) {
        super("Screening overlaps with an existing screening in room " + roomId);
    }
}
