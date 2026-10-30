package com.lsoftware.restauco.entity.delivery.exception;

import com.lsoftware.restauco.entity.exception.DomainException;

/** The map has no address there (or none matching what was written). */
public class PlaceNotFoundException extends DomainException {
    public PlaceNotFoundException() {
        super("PLACE_NOT_FOUND", "The map has no address at that point");
    }
}
