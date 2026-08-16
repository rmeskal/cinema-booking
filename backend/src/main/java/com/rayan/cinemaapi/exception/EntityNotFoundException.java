package com.rayan.cinemaapi.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String entityType, Long id) {
        super(entityType + " with id " + id + " not found");
    }
}
