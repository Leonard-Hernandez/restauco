package com.lsoftware.restauco.infrastructure.floor.gateway;

import com.lsoftware.restauco.entity.floor.gateway.TableGateway;
import com.lsoftware.restauco.entity.floor.model.DiningTable;
import com.lsoftware.restauco.infrastructure.config.db.mapper.DiningTableMapper;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaDiningTableRepository;
import com.lsoftware.restauco.infrastructure.config.db.schema.DiningTableEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TableJpaGateway implements TableGateway {

    private final JpaDiningTableRepository repo;
    private final DiningTableMapper mapper;

    @Override
    public DiningTable save(DiningTable table) {
        DiningTableEntity entity = mapper.toEntity(table);
        if (table.id() != null) {
            DiningTableEntity existing = repo.findOneById(table.id()).orElseThrow();
            entity.setTenantId(existing.getTenantId());
            entity.setCreatedAt(existing.getCreatedAt());
        }
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<DiningTable> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<DiningTable> lockById(Long id) {
        return repo.lockById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<DiningTable> findByQrToken(UUID token) {
        return repo.findByQrToken(token).map(mapper::toDomain);
    }

    @Override
    public List<DiningTable> findAll() {
        return repo.findAllByOrderByNumberAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByNumber(int number, Long exceptId) {
        return exceptId == null ? repo.existsByNumber(number) : repo.existsByNumberAndIdNot(number, exceptId);
    }
}
