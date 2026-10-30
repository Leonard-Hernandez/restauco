package com.lsoftware.restauco.infrastructure.delivery.dto;

import com.lsoftware.restauco.entity.delivery.model.DeliveryOrder;
import com.lsoftware.restauco.infrastructure.order.dto.TicketResponse;
import com.lsoftware.restauco.usecase.delivery.dto.DeliveryView;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A delivery as the waiter sees it: contact, the written address next to what the map found
 * at the pin (and whether they agree), how the customer pays, and its rounds.
 */
public record DeliveryResponse(
        Long id,
        Long sessionId,
        DeliveryStatus status,
        String customerName,
        String phone,
        String address,
        String addressDetail,
        double lat,
        double lng,
        String placeLabel,
        boolean matchesAddress,
        int distanceMeters,
        Long paymentMethodId,
        String paymentMethodName,
        Long cashFor,
        long subtotal,
        long fee,
        long total,
        OffsetDateTime createdAt,
        List<TicketResponse> tickets
) {
    public static DeliveryResponse from(DeliveryView v) {
        DeliveryOrder d = v.delivery();
        return new DeliveryResponse(d.id(), d.sessionId(), DeliveryStatus.of(v), d.customerName(), d.phone(),
                d.address(), d.addressDetail(), d.lat(), d.lng(), d.placeLabel(), d.matchesAddress(),
                d.distanceMeters(), d.paymentMethodId(), d.paymentMethodName(), d.cashFor(), v.subtotal(),
                v.session().deliveryFee(), v.total(), d.createdAt(),
                v.tickets().stream().map(TicketResponse::from).toList());
    }
}
