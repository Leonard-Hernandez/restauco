package com.lsoftware.restauco.infrastructure.delivery.dto;

import com.lsoftware.restauco.entity.floor.model.SessionStatus;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.usecase.delivery.dto.DeliveryView;

/**
 * Where a delivery is, in the customer's words: the round's state plus the bill's
 * (DELIVERED on the round = it left with the courier; the bill closed = paid and delivered).
 */
public enum DeliveryStatus {
    /** Waiting for a waiter to check the order and address */
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    ON_THE_WAY,
    DELIVERED,
    CANCELLED;

    public static DeliveryStatus of(DeliveryView v) {
        if (v.session().status() == SessionStatus.CANCELLED) return CANCELLED;
        if (v.session().status() == SessionStatus.CLOSED) return DELIVERED;
        OrderTicket t = v.current();
        if (t == null) return PENDING;
        return switch (t.status()) {
            case PENDING_APPROVAL -> PENDING;
            case SENT -> CONFIRMED;
            case PREPARING -> PREPARING;
            case READY -> READY;
            case DELIVERED -> ON_THE_WAY;
            case CANCELLED -> CANCELLED;
        };
    }
}
