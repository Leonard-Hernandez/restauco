package com.lsoftware.restauco.entity.order.exception;

import com.lsoftware.restauco.entity.exception.NotFoundException;

public class TicketNotFoundException extends NotFoundException {
    public TicketNotFoundException() {
        super("TICKET_NOT_FOUND", "Order not found");
    }
}
