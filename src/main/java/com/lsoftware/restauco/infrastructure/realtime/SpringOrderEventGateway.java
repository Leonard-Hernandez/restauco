package com.lsoftware.restauco.infrastructure.realtime;

import com.lsoftware.restauco.entity.order.gateway.OrderEventGateway;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.tenant.gateway.CurrentTenantGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringOrderEventGateway implements OrderEventGateway {

    private final ApplicationEventPublisher publisher;
    private final CurrentTenantGateway tenants;

    @Override
    public void ticketChanged(OrderTicket ticket) {
        publisher.publishEvent(new OrderEvents.TicketChanged(tenants.current().id(), ticket));
    }

    @Override
    public void sessionChanged(Long tableId) {
        publisher.publishEvent(new OrderEvents.SessionChanged(tenants.current().id(), tableId));
    }

    @Override
    public void menuChanged() {
        publisher.publishEvent(new OrderEvents.MenuChanged(tenants.current().id()));
    }
}
