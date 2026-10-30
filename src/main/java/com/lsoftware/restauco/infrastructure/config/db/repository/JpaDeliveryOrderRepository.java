package com.lsoftware.restauco.infrastructure.config.db.repository;

import com.lsoftware.restauco.infrastructure.config.db.schema.DeliveryOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaDeliveryOrderRepository extends JpaRepository<DeliveryOrderEntity, Long> {
    Optional<DeliveryOrderEntity> findOneById(Long id);
    Optional<DeliveryOrderEntity> findByToken(UUID token);

    @Query("""
            SELECT d FROM DeliveryOrderEntity d
            WHERE d.sessionId IN (SELECT s.id FROM TableSessionEntity s WHERE s.status = 'OPEN')
            ORDER BY d.createdAt
            """)
    List<DeliveryOrderEntity> findOpen();

    long countByPhoneAndCreatedAtAfter(String phone, OffsetDateTime since);
}
