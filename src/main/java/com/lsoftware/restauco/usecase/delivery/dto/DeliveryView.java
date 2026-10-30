package com.lsoftware.restauco.usecase.delivery.dto;

import com.lsoftware.restauco.entity.delivery.model.DeliveryOrder;
import com.lsoftware.restauco.entity.floor.model.TableSession;
import com.lsoftware.restauco.entity.order.model.OrderTicket;

import java.util.List;

/** A delivery with its bill and rounds (usually one). */
public record DeliveryView(
        DeliveryOrder delivery,
        TableSession session,
        List<OrderTicket> tickets
) {
    public long subtotal() {
        return tickets.stream().mapToLong(OrderTicket::billable).sum();
    }

    /** What the customer pays: the food plus the delivery fee (nothing once rejected). */
    public long total() {
        long subtotal = subtotal();
        return subtotal == 0 ? 0 : subtotal + session.deliveryFee();
    }

    /** The round that tells where the order is: the latest one. */
    public OrderTicket current() {
        return tickets.isEmpty() ? null : tickets.getLast();
    }
}
