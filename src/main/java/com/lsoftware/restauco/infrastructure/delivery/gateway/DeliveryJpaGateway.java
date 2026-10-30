package com.lsoftware.restauco.infrastructure.delivery.gateway;

import com.lsoftware.restauco.entity.delivery.gateway.DeliveryGateway;
import com.lsoftware.restauco.entity.delivery.model.DeliveryOrder;
import com.lsoftware.restauco.infrastructure.config.db.mapper.DeliveryOrderMapper;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaDeliveryOrderRepository;
import com.lsoftware.restauco.infrastructure.config.db.schema.DeliveryOrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeliveryJpaGateway implements DeliveryGateway {

    private final JpaDeliveryOrderRepository repo;
    private final DeliveryOrderMapper mapper;

    @Override
    public DeliveryOrder save(DeliveryOrder delivery) {
        DeliveryOrderEntity entity = mapper.toEntity(delivery);
        if (delivery.id() != null) {
            entity.setTenantId(repo.findOneById(delivery.id()).orElseThrow().getTenantId());
        }
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<DeliveryOrder> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<DeliveryOrder> findByToken(UUID token) {
        return repo.findByToken(token).map(mapper::toDomain);
    }

    @Override
    public List<DeliveryOrder> findOpen() {
        return repo.findOpen().stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countByPhoneSince(String phone, OffsetDateTime since) {
        return repo.countByPhoneAndCreatedAtAfter(phone, since);
    }
}
