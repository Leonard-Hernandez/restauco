package com.lsoftware.restauco.entity.floor.exception;

import com.lsoftware.restauco.entity.exception.ConflictException;

public class SessionClosedException extends ConflictException {
    public SessionClosedException() {
        super("SESSION_CLOSED", "This bill is already closed");
    }
}
