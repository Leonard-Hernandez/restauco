package com.lsoftware.restauco.entity.floor.exception;

import com.lsoftware.restauco.entity.exception.NotFoundException;

public class PaymentMethodNotFoundException extends NotFoundException {
    public PaymentMethodNotFoundException() {
        super("PAYMENT_METHOD_NOT_FOUND", "Payment method not found");
    }
}
