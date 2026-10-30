package com.lsoftware.restauco.entity.delivery.exception;

import com.lsoftware.restauco.entity.exception.DomainException;

/** The map service did not answer: the address could not be checked, which is not the same as wrong. */
public class GeocoderUnavailableException extends DomainException {
    public GeocoderUnavailableException() {
        super("GEOCODER_UNAVAILABLE", "The address could not be checked on the map right now");
    }
}
