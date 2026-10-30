package com.lsoftware.restauco.usecase.order;

import com.lsoftware.restauco.entity.floor.gateway.SessionGateway;
import com.lsoftware.restauco.entity.floor.model.TableSession;
import com.lsoftware.restauco.entity.order.exception.TicketNotFoundException;
import com.lsoftware.restauco.entity.order.gateway.OrderEventGateway;
import com.lsoftware.restauco.entity.order.gateway.TicketGateway;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.order.model.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

/** Moves a ticket along: waiter approves/cancels/delivers (or hands to the courier), kitchen starts/finishes. */
@Component
@RequiredArgsConstructor
public class ChangeTicketUseCase {

    private final TicketGateway tickets;
    private final SessionGateway sessions;
    private final OrderEventGateway events;
    private final Clock clock;

    /** A QR order checked by the waiter goes to the kitchen. */
    @Transactional
    public OrderTicket approve(Long id) {
        return move(id, TicketStatus.SENT);
    }

    @Transactional
    public OrderTicket start(Long id) {
        return move(id, TicketStatus.PREPARING);
    }

    @Transactional
    public OrderTicket ready(Long id) {
        return move(id, TicketStatus.READY);
    }

    @Transactional
    public OrderTicket deliver(Long id) {
        return move(id, TicketStatus.DELIVERED);
    }

    /**
     * Only before the kitchen starts it. A delivery left with nothing to deliver is closed
     * without charging, so it does not wait for a payment that will never come.
     */
    @Transactional
    public OrderTicket cancel(Long id, String reason) {
        OrderTicket ticket = tickets.findById(id).orElseThrow(TicketNotFoundException::new);
        OffsetDateTime now = OffsetDateTime.now(clock);
        OrderTicket cancelled = publish(tickets.save(ticket.cancel(reason, now)));
        if (cancelled.isDelivery()) {
            sessions.findById(cancelled.sessionId())
                    .filter(TableSession::isOpen)
                    .filter(s -> tickets.findBySession(s.id()).stream().allMatch(t -> t.status() == TicketStatus.CANCELLED))
                    .ifPresent(s -> {
                        sessions.save(s.cancel(null, now));
                        events.sessionChanged(null);
                    });
        }
        return cancelled;
    }

    private OrderTicket move(Long id, TicketStatus target) {
        OrderTicket ticket = tickets.findById(id).orElseThrow(TicketNotFoundException::new);
        return publish(tickets.save(ticket.moveTo(target, OffsetDateTime.now(clock))));
    }

    private OrderTicket publish(OrderTicket ticket) {
        events.ticketChanged(ticket);
        return ticket;
    }
}
