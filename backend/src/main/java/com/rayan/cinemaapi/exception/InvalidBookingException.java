package com.rayan.cinemaapi.exception;

public class InvalidBookingException extends RuntimeException {
    public InvalidBookingException() {
        super("Seat does not belong to the screening's room");
    }
}
