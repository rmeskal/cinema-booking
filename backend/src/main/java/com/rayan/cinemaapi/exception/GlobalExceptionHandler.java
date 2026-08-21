package com.rayan.cinemaapi.exception;

import com.rayan.cinemaapi.dto.error.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleEntityNotFound(EntityNotFoundException exception) {
        return new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage()
        );
    }

    @ExceptionHandler(InvalidBookingException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleInvalidBooking(InvalidBookingException exception) {
        return new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                exception.getMessage()
        );
    }

    @ExceptionHandler(ScreeningOverlapException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleScreeningOverlap(ScreeningOverlapException exception) {
        return new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException exception) {
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Request body is invalid."
        );
    }

    @ExceptionHandler(EntityInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEntityInUse(EntityInUseException exception) {
        return new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                exception.getMessage()
        );
    }
}