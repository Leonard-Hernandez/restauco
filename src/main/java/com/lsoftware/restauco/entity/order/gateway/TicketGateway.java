package com.lsoftware.restauco.entity.order.gateway;

import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.order.model.TicketStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketGateway {
    OrderTicket save(OrderTicket ticket);
    Optional<OrderTicket> findById(Long id);
    /** Oldest first. */
    List<OrderTicket> findBySession(Long sessionId);
    List<OrderTicket> findBySessions(Collection<Long> sessionIds);
    /** Oldest first. */
    List<OrderTicket> findByStatusIn(Collection<TicketStatus> statuses);
    long countBySessionAndStatus(Long sessionId, TicketStatus status);
}
