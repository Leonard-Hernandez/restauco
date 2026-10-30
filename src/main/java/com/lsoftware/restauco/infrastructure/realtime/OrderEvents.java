package com.lsoftware.restauco.infrastructure.realtime;

import com.lsoftware.restauco.entity.order.model.OrderTicket;

/** Application events carried from the use cases to the SSE hub (after commit). */
public final class OrderEvents {

    private OrderEvents() {}

    public record TicketChanged(Long tenantId, OrderTicket ticket) {}

    public record SessionChanged(Long tenantId, Long tableId) {}

    public record MenuChanged(Long tenantId) {}
}
