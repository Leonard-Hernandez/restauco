package com.lsoftware.restauco.entity.order.exception;

import com.lsoftware.restauco.entity.exception.ConflictException;

public class InvalidTicketStateException extends ConflictException {
    public InvalidTicketStateException() {
        super("INVALID_TICKET_STATE", "This order already changed state");
    }
}
