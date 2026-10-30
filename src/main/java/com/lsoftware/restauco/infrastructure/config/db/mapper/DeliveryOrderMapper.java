package com.lsoftware.restauco.infrastructure.config.db.mapper;

import com.lsoftware.restauco.entity.delivery.model.DeliveryOrder;
import com.lsoftware.restauco.infrastructure.config.db.schema.DeliveryOrderEntity;
import org.springframework.stereotype.Component;

@Component
public class DeliveryOrderMapper {

    public DeliveryOrder toDomain(DeliveryOrderEntity e) {
        return new DeliveryOrder(e.getId(), e.getSessionId(), e.getToken(), e.getCustomerName(), e.getPhone(),
                e.getAddress(), e.getAddressDetail(), e.getLat(), e.getLng(), e.getPlaceLabel(), e.isMatchesAddress(),
                e.getDistanceMeters(), e.getPaymentMethodId(), e.getPaymentMethodName(), e.getCashFor(), e.getCreatedAt());
    }

    public DeliveryOrderEntity toEntity(DeliveryOrder d) {
        return DeliveryOrderEntity.builder()
                .id(d.id())
                .sessionId(d.sessionId())
                .token(d.token())
                .customerName(d.customerName())
                .phone(d.phone())
                .address(d.address())
                .addressDetail(d.addressDetail())
                .lat(d.lat())
                .lng(d.lng())
                .placeLabel(d.placeLabel())
                .matchesAddress(d.matchesAddress())
                .distanceMeters(d.distanceMeters())
                .paymentMethodId(d.paymentMethodId())
                .paymentMethodName(d.paymentMethodName())
                .cashFor(d.cashFor())
                .createdAt(d.createdAt())
                .build();
    }
}
