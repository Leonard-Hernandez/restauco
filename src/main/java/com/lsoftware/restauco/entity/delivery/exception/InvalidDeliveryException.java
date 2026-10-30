package com.lsoftware.restauco.entity.delivery.exception;

import com.lsoftware.restauco.entity.exception.DomainException;

/** Missing or wrong contact details, payment, or an order below the minimum. */
public class InvalidDeliveryException extends DomainException {
    public InvalidDeliveryException(String code, String message) {
        super(code, message);
    }
}
