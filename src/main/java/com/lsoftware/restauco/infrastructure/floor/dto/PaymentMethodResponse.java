package com.lsoftware.restauco.infrastructure.floor.dto;

import com.lsoftware.restauco.entity.floor.model.PaymentMethod;

public record PaymentMethodResponse(Long id, String name, int position, boolean active) {
    public static PaymentMethodResponse from(PaymentMethod m) {
        return new PaymentMethodResponse(m.id(), m.name(), m.position(), m.active());
    }
}
