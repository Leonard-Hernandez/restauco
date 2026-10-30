package com.lsoftware.restauco.entity.delivery.gateway;

import com.lsoftware.restauco.entity.delivery.model.DeliveryOrder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryGateway {
    DeliveryOrder save(DeliveryOrder delivery);
    Optional<DeliveryOrder> findById(Long id);
    Optional<DeliveryOrder> findByToken(UUID token);
    /** Deliveries whose bill is still open, oldest first. */
    List<DeliveryOrder> findOpen();
    long countByPhoneSince(String phone, OffsetDateTime since);
}
