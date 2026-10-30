package com.lsoftware.restauco.entity.order.gateway;

import com.lsoftware.restauco.entity.order.model.OrderTicket;

/**
 * Pushes changes to the open waiter, kitchen and table screens. Delivered only once the
 * surrounding transaction commits, so nobody sees an order that was rolled back.
 */
public interface OrderEventGateway {
    void ticketChanged(OrderTicket ticket);
    /** A bill opened or closed: the floor map must refresh. tableId is null for a delivery. */
    void sessionChanged(Long tableId);
    /** Something ran out or came back: screens holding the menu must reload it. */
    void menuChanged();
}
