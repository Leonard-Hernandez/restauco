package com.lsoftware.restauco.entity.order.exception;

import com.lsoftware.restauco.entity.exception.TooManyRequestsException;

/** The table QR already has orders waiting for a waiter; stops a leaked QR from flooding the floor. */
public class TooManyPendingOrdersException extends TooManyRequestsException {
    public TooManyPendingOrdersException() {
        super("TOO_MANY_PENDING", "This table already has orders waiting for the waiter");
    }
}
