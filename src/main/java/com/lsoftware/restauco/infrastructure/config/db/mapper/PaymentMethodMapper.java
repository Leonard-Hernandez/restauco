package com.lsoftware.restauco.infrastructure.config.db.mapper;

import com.lsoftware.restauco.entity.floor.model.PaymentMethod;
import com.lsoftware.restauco.infrastructure.config.db.schema.PaymentMethodEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentMethodMapper {

    public PaymentMethod toDomain(PaymentMethodEntity e) {
        return new PaymentMethod(e.getId(), e.getName(), e.getPosition(), e.isActive());
    }

    public PaymentMethodEntity toEntity(PaymentMethod m) {
        return PaymentMethodEntity.builder()
                .id(m.id())
                .name(m.name())
                .position(m.position())
                .active(m.active())
                .build();
    }
}
