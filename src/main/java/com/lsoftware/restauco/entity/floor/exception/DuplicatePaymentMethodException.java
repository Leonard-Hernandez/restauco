package com.lsoftware.restauco.entity.floor.exception;

import com.lsoftware.restauco.entity.exception.ConflictException;

public class DuplicatePaymentMethodException extends ConflictException {
    public DuplicatePaymentMethodException() {
        super("DUPLICATE_NAME", "There is already a payment method with that name");
    }
}
