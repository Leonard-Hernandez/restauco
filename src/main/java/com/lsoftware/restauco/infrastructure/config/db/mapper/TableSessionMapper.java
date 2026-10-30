package com.lsoftware.restauco.infrastructure.config.db.mapper;

import com.lsoftware.restauco.entity.floor.model.SessionStatus;
import com.lsoftware.restauco.entity.floor.model.TableSession;
import com.lsoftware.restauco.infrastructure.config.db.schema.TableSessionEntity;
import org.springframework.stereotype.Component;

@Component
public class TableSessionMapper {

    public TableSession toDomain(TableSessionEntity e) {
        return new TableSession(e.getId(), e.getTableId(), SessionStatus.valueOf(e.getStatus()), e.getOpenedAt(),
                e.getClosedAt(), e.getClosedBy(), e.getPaymentMethodId(), e.getPaymentMethodName(), e.getTotal(), e.getPayments(),
                e.getDeliveryFee());
    }

    public TableSessionEntity toEntity(TableSession s) {
        return TableSessionEntity.builder()
                .id(s.id())
                .tableId(s.tableId())
                .status(s.status().name())
                .openedAt(s.openedAt())
                .closedAt(s.closedAt())
                .closedBy(s.closedBy())
                .paymentMethodId(s.paymentMethodId())
                .paymentMethodName(s.paymentMethodName())
                .total(s.total())
                .payments(s.payments())
                .deliveryFee(s.deliveryFee())
                .build();
    }
}
