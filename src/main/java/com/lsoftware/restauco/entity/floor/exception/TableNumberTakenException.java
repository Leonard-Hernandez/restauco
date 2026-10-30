package com.lsoftware.restauco.entity.floor.exception;

import com.lsoftware.restauco.entity.exception.ConflictException;

public class TableNumberTakenException extends ConflictException {
    public TableNumberTakenException() {
        super("TABLE_NUMBER_TAKEN", "There is already a table with that number");
    }
}
