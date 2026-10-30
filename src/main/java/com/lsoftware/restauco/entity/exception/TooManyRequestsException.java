package com.lsoftware.restauco.entity.exception;

/** Too many requests from the same guest, table or phone in a short time (429). */
public class TooManyRequestsException extends DomainException {
    public TooManyRequestsException(String code, String message) {
        super(code, message);
    }
}
