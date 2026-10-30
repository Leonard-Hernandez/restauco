package com.lsoftware.restauco.infrastructure.floor.gateway;

import com.lsoftware.restauco.entity.floor.gateway.PaymentMethodGateway;
import com.lsoftware.restauco.entity.floor.model.PaymentMethod;
import com.lsoftware.restauco.infrastructure.config.db.mapper.PaymentMethodMapper;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaPaymentMethodRepository;
import com.lsoftware.restauco.infrastructure.config.db.schema.PaymentMethodEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentMethodJpaGateway implements PaymentMethodGateway {

    private final JpaPaymentMethodRepository repo;
    private final PaymentMethodMapper mapper;

    @Override
    public PaymentMethod save(PaymentMethod method) {
        PaymentMethodEntity entity = mapper.toEntity(method);
        if (method.id() != null) {
            entity.setTenantId(repo.findOneById(method.id()).orElseThrow().getTenantId());
        }
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<PaymentMethod> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public List<PaymentMethod> findAll() {
        return repo.findAllByOrderByPositionAscIdAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name, Long exceptId) {
        return exceptId == null ? repo.existsByNameIgnoreCase(name) : repo.existsByNameIgnoreCaseAndIdNot(name, exceptId);
    }
}
