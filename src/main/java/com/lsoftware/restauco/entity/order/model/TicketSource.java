package com.lsoftware.restauco.entity.order.model;

public enum TicketSource {
    /** Taken by a waiter: goes straight to the kitchen. */
    WAITER,
    /** Placed by the guest from the table QR: a waiter approves it first. */
    QR,
    /** Ordered for delivery from the website: a waiter confirms it first. */
    DELIVERY
}
