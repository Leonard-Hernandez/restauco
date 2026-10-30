package com.lsoftware.restauco.infrastructure.tenant.gateway;

import com.lsoftware.restauco.entity.delivery.model.DeliverySettings;
import com.lsoftware.restauco.entity.tenant.gateway.TenantGateway;
import com.lsoftware.restauco.entity.tenant.model.Tenant;
import com.lsoftware.restauco.infrastructure.config.db.mapper.TenantMapper;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaTenantRepository;
import com.lsoftware.restauco.infrastructure.config.db.schema.TenantEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantJpaGateway implements TenantGateway {

    private final JpaTenantRepository repo;
    private final TenantMapper mapper;

    @Override
    public Optional<Tenant> findBySlug(String slug) {
        return repo.findBySlug(slug).map(mapper::toDomain);
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        return repo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Tenant saveDelivery(Long id, DeliverySettings delivery) {
        TenantEntity entity = repo.findById(id).orElseThrow();
        entity.setDelivery(delivery);
        return mapper.toDomain(repo.save(entity));
    }
}
