package com.lsoftware.restauco.entity.floor.exception;

import com.lsoftware.restauco.entity.exception.NotFoundException;

public class SessionNotFoundException extends NotFoundException {
    public SessionNotFoundException() {
        super("SESSION_NOT_FOUND", "Bill not found");
    }
}
