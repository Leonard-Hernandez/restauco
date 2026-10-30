package com.lsoftware.restauco.entity.floor.exception;

import com.lsoftware.restauco.entity.exception.DomainException;

public class PaymentsDoNotMatchTotalException extends DomainException {
    public PaymentsDoNotMatchTotalException(long paid, long total) {
        super("PAYMENTS_DO_NOT_MATCH_TOTAL", "The payments add up to " + paid + " but the bill is " + total);
    }
}
