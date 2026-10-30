package com.lsoftware.restauco.entity.floor.exception;

import com.lsoftware.restauco.entity.exception.DomainException;

public class InvalidPaymentsException extends DomainException {
    public InvalidPaymentsException(String message) {
        super("INVALID_PAYMENTS", message);
    }
}
