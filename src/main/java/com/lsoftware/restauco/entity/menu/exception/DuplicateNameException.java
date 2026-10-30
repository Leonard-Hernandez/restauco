package com.lsoftware.restauco.entity.menu.exception;

import com.lsoftware.restauco.entity.exception.ConflictException;

public class DuplicateNameException extends ConflictException {
    public DuplicateNameException() {
        super("DUPLICATE_NAME", "There is already one with that name");
    }
}
