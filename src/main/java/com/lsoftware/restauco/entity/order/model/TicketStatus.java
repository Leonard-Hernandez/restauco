package com.lsoftware.restauco.entity.order.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Life of a ticket:
 * <pre>
 * PENDING_APPROVAL (QR order) -> SENT -> PREPARING -> READY -> DELIVERED
 *                  \-> CANCELLED  \-> READY (drinks)
 *                                  \-> CANCELLED
 * </pre>
 */
public enum TicketStatus {
    PENDING_APPROVAL,
    SENT,
    PREPARING,
    READY,
    DELIVERED,
    CANCELLED;

    public static final Set<TicketStatus> KITCHEN = EnumSet.of(SENT, PREPARING, READY);

    public boolean canMoveTo(TicketStatus target) {
        return switch (this) {
            case PENDING_APPROVAL -> target == SENT || target == CANCELLED;
            case SENT -> target == PREPARING || target == READY || target == CANCELLED;
            case PREPARING -> target == READY;
            case READY -> target == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    /** Still needs someone to act on it (the bill cannot close yet). */
    public boolean active() {
        return this != DELIVERED && this != CANCELLED;
    }
}
