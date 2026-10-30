package com.lsoftware.restauco.infrastructure.order.gateway;

import com.lsoftware.restauco.entity.order.gateway.TicketGateway;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.order.model.TicketStatus;
import com.lsoftware.restauco.infrastructure.config.db.mapper.OrderTicketMapper;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaOrderTicketRepository;
import com.lsoftware.restauco.infrastructure.config.db.schema.OrderTicketEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TicketJpaGateway implements TicketGateway {

    private final JpaOrderTicketRepository repo;
    private final OrderTicketMapper mapper;

    // A stale version (someone else moved the ticket meanwhile) fails the merge with a 409
    @Override
    public OrderTicket save(OrderTicket ticket) {
        OrderTicketEntity entity = mapper.toEntity(ticket);
        if (ticket.id() != null) {
            entity.setTenantId(repo.findOneById(ticket.id()).orElseThrow().getTenantId());
        }
        return mapper.toDomain(repo.saveAndFlush(entity));
    }

    @Override
    public Optional<OrderTicket> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public List<OrderTicket> findBySession(Long sessionId) {
        return repo.findBySessionIdOrderByCreatedAtAsc(sessionId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrderTicket> findBySessions(Collection<Long> sessionIds) {
        return repo.findBySessionIdInOrderByCreatedAtAsc(sessionIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<OrderTicket> findByStatusIn(Collection<TicketStatus> statuses) {
        return repo.findByStatusInOrderByCreatedAtAsc(statuses.stream().map(Enum::name).toList()).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public long countBySessionAndStatus(Long sessionId, TicketStatus status) {
        return repo.countBySessionIdAndStatus(sessionId, status.name());
    }
}
