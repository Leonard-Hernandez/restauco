package com.lsoftware.restauco.entity.tenant.gateway;

import com.lsoftware.restauco.entity.delivery.model.DeliverySettings;
import com.lsoftware.restauco.entity.tenant.model.Tenant;

import java.util.Optional;

public interface TenantGateway {
    Optional<Tenant> findBySlug(String slug);
    Optional<Tenant> findById(Long id);
    Tenant saveDelivery(Long id, DeliverySettings delivery);
}
