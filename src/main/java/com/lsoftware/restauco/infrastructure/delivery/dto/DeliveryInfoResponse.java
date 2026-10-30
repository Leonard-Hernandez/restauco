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

    public static DeliveryInfoResponse from(DeliverySettings s, List<PaymentMethod> methods) {
        return new DeliveryInfoResponse(s.open(), s.lat(), s.lng(), s.radiusKm(), s.fee(), s.minOrder(),
                methods.stream().filter(m -> s.accepts(m.id())).map(m -> new Method(m.id(), m.name())).toList());
    }
}
