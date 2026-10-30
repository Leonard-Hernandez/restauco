package com.lsoftware.restauco.entity.member.exception;

import com.lsoftware.restauco.entity.exception.DomainException;

/** An owner removing or demoting themselves could lock the restaurant out of its panel. */
public class SelfChangeException extends DomainException {
    public SelfChangeException() {
        super("SELF_CHANGE", "You cannot remove or demote yourself");
    }
}
