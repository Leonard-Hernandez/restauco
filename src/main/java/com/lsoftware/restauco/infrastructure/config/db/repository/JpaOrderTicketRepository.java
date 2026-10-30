package com.lsoftware.restauco.infrastructure.config.db.repository;

import com.lsoftware.restauco.infrastructure.config.db.schema.OrderTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaOrderTicketRepository extends JpaRepository<OrderTicketEntity, Long> {
    Optional<OrderTicketEntity> findOneById(Long id);
    List<OrderTicketEntity> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    List<OrderTicketEntity> findBySessionIdInOrderByCreatedAtAsc(Collection<Long> sessionIds);
    List<OrderTicketEntity> findByStatusInOrderByCreatedAtAsc(Collection<String> statuses);
    long countBySessionIdAndStatus(Long sessionId, String status);
}
