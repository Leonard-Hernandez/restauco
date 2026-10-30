package com.lsoftware.restauco.entity.floor.exception;

import com.lsoftware.restauco.entity.exception.ConflictException;

public class SessionHasActiveTicketsException extends ConflictException {
    public SessionHasActiveTicketsException() {
        super("SESSION_HAS_ACTIVE_TICKETS", "Deliver or cancel every order before closing the bill");
    }
}
