package com.lsoftware.restauco.entity.menu.exception;

import com.lsoftware.restauco.entity.exception.DomainException;

public class InvalidModifiersException extends DomainException {
    public InvalidModifiersException(String message) {
        super("INVALID_MODIFIERS", message);
    }
}
