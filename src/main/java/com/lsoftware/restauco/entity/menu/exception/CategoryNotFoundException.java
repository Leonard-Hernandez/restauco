package com.lsoftware.restauco.entity.menu.exception;

import com.lsoftware.restauco.entity.exception.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {
    public CategoryNotFoundException() {
        super("CATEGORY_NOT_FOUND", "Category not found");
    }
}
