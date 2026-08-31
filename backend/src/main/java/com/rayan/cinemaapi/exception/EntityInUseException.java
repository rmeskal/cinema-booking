package com.rayan.cinemaapi.exception;

public class EntityInUseException extends RuntimeException {
    public EntityInUseException(String entity, Object id) {
        super(entity + " with id " + id + " is still in use");
    }
}
