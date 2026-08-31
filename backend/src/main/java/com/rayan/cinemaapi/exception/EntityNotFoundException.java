package com.rayan.cinemaapi.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String entityType, Object id) {
        super(entityType + " with id " + id + " was not found");
    }
}
