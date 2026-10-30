package com.lsoftware.restauco.entity.order.exception;

import com.lsoftware.restauco.entity.exception.DomainException;

/** The order does not match the menu (unknown dish, bad modifiers...). */
public class InvalidOrderException extends DomainException {
    public InvalidOrderException(String code, String message) {
        super(code, message);
    }
}
