package com.lsoftware.restauco.infrastructure.config.db.repository;

import com.lsoftware.restauco.infrastructure.config.db.schema.TableSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaTableSessionRepository extends JpaRepository<TableSessionEntity, Long> {
    Optional<TableSessionEntity> findOneById(Long id);
    Optional<TableSessionEntity> findByTableIdAndStatus(Long tableId, String status);
    List<TableSessionEntity> findByStatusAndTableIdIsNotNull(String status);
}
