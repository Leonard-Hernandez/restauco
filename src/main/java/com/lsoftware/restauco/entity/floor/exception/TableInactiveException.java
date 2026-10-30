package com.lsoftware.restauco.entity.floor.exception;

import com.lsoftware.restauco.entity.exception.ConflictException;

public class TableInactiveException extends ConflictException {
    public TableInactiveException() {
        super("TABLE_INACTIVE", "This table is not taking orders");
    }
}
