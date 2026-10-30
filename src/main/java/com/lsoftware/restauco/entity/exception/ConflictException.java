package com.lsoftware.restauco.entity.exception;

/** The request is valid but clashes with the current state (409). */
public class ConflictException extends DomainException {
    public ConflictException(String code, String message) {
        super(code, message);
    }
}
