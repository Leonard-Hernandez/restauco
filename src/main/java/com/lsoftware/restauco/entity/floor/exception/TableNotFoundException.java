package com.lsoftware.restauco.entity.floor.exception;

import com.lsoftware.restauco.entity.exception.NotFoundException;

public class TableNotFoundException extends NotFoundException {
    public TableNotFoundException() {
        super("TABLE_NOT_FOUND", "Table not found");
    }
}
