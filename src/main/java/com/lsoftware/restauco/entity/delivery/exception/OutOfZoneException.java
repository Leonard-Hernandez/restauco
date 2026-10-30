package com.lsoftware.restauco.entity.delivery.exception;

import com.lsoftware.restauco.entity.exception.DomainException;

public class OutOfZoneException extends DomainException {
    public OutOfZoneException() {
        super("OUT_OF_ZONE", "That address is too far for delivery");
    }
}
