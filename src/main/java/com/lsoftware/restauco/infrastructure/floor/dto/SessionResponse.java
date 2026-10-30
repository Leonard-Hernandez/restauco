package com.lsoftware.restauco.infrastructure.floor.dto;

import com.lsoftware.restauco.entity.floor.model.Payment;
import com.lsoftware.restauco.entity.floor.model.SessionStatus;
import com.lsoftware.restauco.entity.floor.model.TableSession;

import java.time.OffsetDateTime;
import java.util.List;

public record SessionResponse(
        Long id,
        Long tableId,
        SessionStatus status,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt,
        String paymentMethodName,
        Long total,
        List<Payment> payments,
        long deliveryFee
) {
    public static SessionResponse from(TableSession s) {
        return s == null ? null : new SessionResponse(s.id(), s.tableId(), s.status(), s.openedAt(), s.closedAt(),
                s.paymentMethodName(), s.total(), s.payments(), s.deliveryFee());
    }
}
