package com.lsoftware.restauco.entity.menu.exception;

import com.lsoftware.restauco.entity.exception.ConflictException;

public class CategoryNotEmptyException extends ConflictException {
    public CategoryNotEmptyException() {
        super("CATEGORY_NOT_EMPTY", "Move or delete the dishes of this category first");
    }
}
