package com.lsoftware.restauco.entity.menu.exception;

import com.lsoftware.restauco.entity.exception.NotFoundException;

public class TagNotFoundException extends NotFoundException {
    public TagNotFoundException() {
        super("TAG_NOT_FOUND", "Tag not found");
    }
}
