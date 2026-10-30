package com.lsoftware.restauco.infrastructure.delivery.dto;

import com.lsoftware.restauco.entity.delivery.model.DeliverySettings;
import com.lsoftware.restauco.entity.floor.model.PaymentMethod;

import java.util.List;

/** What the delivery page needs before the customer orders. */
public record DeliveryInfoResponse(
        boolean open,
        Double lat,
        Double lng,
        double radiusKm,
        long fee,
        long minOrder,
        List<Method> paymentMethods
) {
    public record Method(Long id, String name) {}

    /** {@code methods} are the active ones; with none of them taken for delivery it shows as closed. */
    public static DeliveryInfoResponse from(DeliverySettings s, List<PaymentMethod> methods) {
        List<Method> accepted = methods.stream().filter(m -> s.accepts(m.id())).map(m -> new Method(m.id(), m.name())).toList();
        return new DeliveryInfoResponse(s.open() && !accepted.isEmpty(), s.lat(), s.lng(), s.radiusKm(), s.fee(),
                s.minOrder(), accepted);
    }
}
