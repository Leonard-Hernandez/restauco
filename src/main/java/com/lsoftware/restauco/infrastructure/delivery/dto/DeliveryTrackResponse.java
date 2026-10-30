package com.lsoftware.restauco.infrastructure.delivery.dto;

import com.lsoftware.restauco.entity.delivery.model.DeliveryOrder;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.infrastructure.order.dto.TicketItemResponse;
import com.lsoftware.restauco.usecase.delivery.dto.DeliveryView;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** What the customer sees while following their order (only with the token they got). */
public record DeliveryTrackResponse(
        UUID token,
        DeliveryStatus status,
        String customerName,
        String address,
        String addressDetail,
        String placeLabel,
        String paymentMethodName,
        Long cashFor,
        List<TicketItemResponse> items,
        long subtotal,
        long fee,
        long total,
        OffsetDateTime createdAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime readyAt,
        OffsetDateTime dispatchedAt,
        OffsetDateTime deliveredAt,
        String cancelReason
) {
    public static DeliveryTrackResponse from(DeliveryView v) {
        DeliveryOrder d = v.delivery();
        List<OrderTicket> live = v.tickets().stream().filter(t -> t.billable() > 0).toList();
        OrderTicket current = v.current();
        return new DeliveryTrackResponse(d.token(), DeliveryStatus.of(v), d.customerName(), d.address(),
                d.addressDetail(), d.placeLabel(), d.paymentMethodName(), d.cashFor(),
                (live.isEmpty() ? v.tickets() : live).stream().flatMap(t -> t.items().stream()).map(TicketItemResponse::from).toList(),
                v.subtotal(), v.session().deliveryFee(), v.total(), d.createdAt(),
                current != null ? current.sentAt() : null,
                current != null ? current.readyAt() : null,
                current != null ? current.deliveredAt() : null,
                v.session().closedAt(),
                current != null ? current.cancelReason() : null);
    }
}
