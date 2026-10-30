package com.lsoftware.restauco.infrastructure.order.dto;

import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.order.model.TicketSource;
import com.lsoftware.restauco.entity.order.model.TicketStatus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A ticket as staff screens and the table's guests see it (no staff identity). A delivery has
 * deliveryId and no table.
 */
public record TicketResponse(
        Long id,
        Long sessionId,
        Long tableId,
        Integer tableNumber,
        Long deliveryId,
        TicketStatus status,
        TicketSource source,
        List<TicketItemResponse> items,
        long total,
        OffsetDateTime createdAt,
        OffsetDateTime sentAt,
        OffsetDateTime startedAt,
        OffsetDateTime readyAt,
        OffsetDateTime deliveredAt,
        OffsetDateTime cancelledAt,
        String cancelReason
) {
    public static TicketResponse from(OrderTicket t) {
        return new TicketResponse(t.id(), t.sessionId(), t.tableId(), t.tableNumber(), t.deliveryId(), t.status(), t.source(),
                t.items().stream().map(TicketItemResponse::from).toList(), t.total(), t.createdAt(), t.sentAt(),
                t.startedAt(), t.readyAt(), t.deliveredAt(), t.cancelledAt(), t.cancelReason());
    }
}
