package com.lsoftware.restauco.infrastructure.floor.gateway;

import com.lsoftware.restauco.entity.floor.gateway.SessionGateway;
import com.lsoftware.restauco.entity.floor.model.SessionStatus;
import com.lsoftware.restauco.entity.floor.model.TableSession;
import com.lsoftware.restauco.infrastructure.config.db.mapper.TableSessionMapper;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaTableSessionRepository;
import com.lsoftware.restauco.infrastructure.config.db.schema.TableSessionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SessionJpaGateway implements SessionGateway {

    private final JpaTableSessionRepository repo;
    private final TableSessionMapper mapper;

    @Override
    public TableSession save(TableSession session) {
        TableSessionEntity entity = mapper.toEntity(session);
        if (session.id() != null) {
            entity.setTenantId(repo.findOneById(session.id()).orElseThrow().getTenantId());
        }
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<TableSession> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<TableSession> findOpenByTable(Long tableId) {
        return repo.findByTableIdAndStatus(tableId, SessionStatus.OPEN.name()).map(mapper::toDomain);
    }

    @Override
    public List<TableSession> findAllOpen() {
        return repo.findByStatusAndTableIdIsNotNull(SessionStatus.OPEN.name()).stream().map(mapper::toDomain).toList();
    }
}
