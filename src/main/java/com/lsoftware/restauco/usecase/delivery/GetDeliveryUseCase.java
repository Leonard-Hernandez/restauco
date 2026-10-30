package com.lsoftware.restauco.usecase.delivery;

import com.lsoftware.restauco.entity.delivery.exception.DeliveryNotFoundException;
import com.lsoftware.restauco.entity.delivery.gateway.DeliveryGateway;
import com.lsoftware.restauco.entity.delivery.model.DeliveryOrder;
import com.lsoftware.restauco.entity.floor.gateway.SessionGateway;
import com.lsoftware.restauco.entity.floor.model.TableSession;
import com.lsoftware.restauco.entity.order.gateway.TicketGateway;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.usecase.delivery.dto.DeliveryView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetDeliveryUseCase {

    private final DeliveryGateway deliveries;
    private final SessionGateway sessions;
    private final TicketGateway tickets;

    /** The customer following their order. */
    @Transactional(readOnly = true)
    public DeliveryView byToken(UUID token) {
        DeliveryOrder delivery = deliveries.findByToken(token).orElseThrow(DeliveryNotFoundException::new);
        TableSession session = sessions.findById(delivery.sessionId()).orElseThrow(DeliveryNotFoundException::new);
        return new DeliveryView(delivery, session, tickets.findBySession(session.id()));
    }

    /** The waiter's list: every delivery not charged yet, oldest first. */
    @Transactional(readOnly = true)
    public List<DeliveryView> open() {
        List<DeliveryOrder> open = deliveries.findOpen();
        if (open.isEmpty()) return List.of();
        List<Long> sessionIds = open.stream().map(DeliveryOrder::sessionId).toList();
        Map<Long, List<OrderTicket>> bySession = tickets.findBySessions(sessionIds).stream()
                .collect(Collectors.groupingBy(OrderTicket::sessionId));
        return open.stream().flatMap(d -> sessions.findById(d.sessionId()).stream()
                        .map(s -> new DeliveryView(d, s, bySession.getOrDefault(s.id(), List.of()))))
                .toList();
    }
}
