package com.lsoftware.restauco.entity.delivery.exception;

import com.lsoftware.restauco.entity.exception.ConflictException;

public class DeliveryClosedException extends ConflictException {
    public DeliveryClosedException() {
        super("DELIVERY_CLOSED", "This restaurant is not taking delivery orders now");
    }
}
