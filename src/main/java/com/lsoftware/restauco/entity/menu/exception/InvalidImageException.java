package com.lsoftware.restauco.entity.menu.exception;

import com.lsoftware.restauco.entity.exception.DomainException;

public class InvalidImageException extends DomainException {
    public InvalidImageException() {
        super("INVALID_IMAGE", "Upload a JPG, PNG or WebP photo of up to 5 MB");
    }
}
