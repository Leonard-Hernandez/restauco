package com.lsoftware.restauco.entity.order.model;

import com.lsoftware.restauco.entity.order.exception.InvalidTicketStateException;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * One round of orders, as the kitchen sees it: for a table (tableId, tableNumber) or for a
 * delivery (deliveryId, no table).
 */
public record OrderTicket(
        Long id,
        Long sessionId,
        Long tableId,
        Integer tableNumber,
        Long deliveryId,
        TicketStatus status,
        TicketSource source,
        String waiterSub,
        List<TicketItem> items,
        long total,
        OffsetDateTime createdAt,
        OffsetDateTime sentAt,
        OffsetDateTime startedAt,
        OffsetDateTime readyAt,
        OffsetDateTime deliveredAt,
        OffsetDateTime cancelledAt,
        String cancelReason,
        Long version
) {
    public OrderTicket {
        items = items != null ? List.copyOf(items) : List.of();
    }

    public static OrderTicket place(Long sessionId, Long tableId, int tableNumber, TicketSource source,
                                    String waiterSub, List<TicketItem> items, OffsetDateTime now) {
        boolean direct = source == TicketSource.WAITER;
        return create(sessionId, tableId, tableNumber, null, source, waiterSub, items, direct, now);
    }

    /** Ordered from the website: a waiter confirms it (and the address) before the kitchen sees it. */
    public static OrderTicket forDelivery(Long sessionId, Long deliveryId, List<TicketItem> items, OffsetDateTime now) {
        return create(sessionId, null, null, deliveryId, TicketSource.DELIVERY, null, items, false, now);
    }

    private static OrderTicket create(Long sessionId, Long tableId, Integer tableNumber, Long deliveryId,
                                      TicketSource source, String waiterSub, List<TicketItem> items, boolean direct,
                                      OffsetDateTime now) {
        long total = items.stream().mapToLong(TicketItem::lineTotal).sum();
        return new OrderTicket(null, sessionId, tableId, tableNumber, deliveryId,
                direct ? TicketStatus.SENT : TicketStatus.PENDING_APPROVAL, source, waiterSub, items, total,
                now, direct ? now : null, null, null, null, null, null, null);
    }

    public boolean isDelivery() {
        return deliveryId != null;
    }

    /** Moves to the next state, stamping when it happened. */
    public OrderTicket moveTo(TicketStatus target, OffsetDateTime now) {
        if (!status.canMoveTo(target)) {
            throw new InvalidTicketStateException();
        }
        return new OrderTicket(id, sessionId, tableId, tableNumber, deliveryId, target, source, waiterSub, items, total,
                createdAt,
                target == TicketStatus.SENT ? now : sentAt,
                target == TicketStatus.PREPARING ? now : startedAt,
                target == TicketStatus.READY ? now : readyAt,
                target == TicketStatus.DELIVERED ? now : deliveredAt,
                target == TicketStatus.CANCELLED ? now : cancelledAt,
                cancelReason, version);
    }

    public OrderTicket cancel(String reason, OffsetDateTime now) {
        OrderTicket cancelled = moveTo(TicketStatus.CANCELLED, now);
        return new OrderTicket(id, sessionId, tableId, tableNumber, deliveryId, cancelled.status(), source, waiterSub,
                items, total, createdAt, sentAt, startedAt, readyAt, deliveredAt, cancelled.cancelledAt(),
                reason != null && !reason.isBlank() ? reason.strip() : null, version);
    }

    /** What this ticket adds to the bill (cancelled tickets are not charged). */
    public long billable() {
        return status == TicketStatus.CANCELLED ? 0 : total;
    }
}
