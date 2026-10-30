package com.lsoftware.restauco.entity.exception;

/** Something the request names does not exist in the current restaurant (404). */
public class NotFoundException extends DomainException {
    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
