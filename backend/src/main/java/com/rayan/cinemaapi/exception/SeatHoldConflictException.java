package com.rayan.cinemaapi.exception;

public class SeatHoldConflictException extends RuntimeException {
    public SeatHoldConflictException(String message) {
        super(message);
    }
}
