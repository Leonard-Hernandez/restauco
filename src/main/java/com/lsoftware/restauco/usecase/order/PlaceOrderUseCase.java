package com.lsoftware.restauco.usecase.order;

import com.lsoftware.restauco.entity.floor.exception.TableInactiveException;
import com.lsoftware.restauco.entity.floor.exception.TableNotFoundException;
import com.lsoftware.restauco.entity.floor.gateway.SessionGateway;
import com.lsoftware.restauco.entity.floor.gateway.TableGateway;
import com.lsoftware.restauco.entity.floor.model.DiningTable;
import com.lsoftware.restauco.entity.floor.model.TableSession;
import com.lsoftware.restauco.entity.order.exception.TooManyPendingOrdersException;
import com.lsoftware.restauco.entity.order.gateway.OrderEventGateway;
import com.lsoftware.restauco.entity.order.gateway.TicketGateway;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.order.model.TicketItem;
import com.lsoftware.restauco.entity.order.model.TicketSource;
import com.lsoftware.restauco.entity.order.model.TicketStatus;
import com.lsoftware.restauco.usecase.order.dto.OrderLine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A new round for a table. The first round opens the table's bill. Waiter orders go straight
 * to the kitchen; QR orders wait for a waiter to approve them.
 */
@Component
public class PlaceOrderUseCase {

    private final TableGateway tables;
    private final SessionGateway sessions;
    private final TicketGateway tickets;
    private final OrderPricer pricer;
    private final OrderEventGateway events;
    private final Clock clock;
    private final int maxPendingPerTable;

    public PlaceOrderUseCase(TableGateway tables, SessionGateway sessions, TicketGateway tickets, OrderPricer pricer,
                             OrderEventGateway events, Clock clock,
                             @Value("${restauco.qr.max-pending-per-table:3}") int maxPendingPerTable) {
        this.tables = tables;
        this.sessions = sessions;
        this.tickets = tickets;
        this.pricer = pricer;
        this.events = events;
        this.clock = clock;
        this.maxPendingPerTable = maxPendingPerTable;
    }

    @Transactional
    public OrderTicket byWaiter(Long tableId, List<OrderLine> lines, String waiterSub) {
        DiningTable table = tables.lockById(tableId).orElseThrow(TableNotFoundException::new);
        return place(table, TicketSource.WAITER, waiterSub, pricer.price(lines));
    }

    @Transactional
    public OrderTicket byGuest(UUID qrToken, List<OrderLine> lines) {
        DiningTable found = tables.findByQrToken(qrToken).orElseThrow(TableNotFoundException::new);
        DiningTable table = tables.lockById(found.id()).orElseThrow(TableNotFoundException::new);
        if (!table.active()) throw new TableInactiveException();
        List<TicketItem> items = pricer.price(lines);
        sessions.findOpenByTable(table.id()).ifPresent(session -> {
            if (tickets.countBySessionAndStatus(session.id(), TicketStatus.PENDING_APPROVAL) >= maxPendingPerTable) {
                throw new TooManyPendingOrdersException();
            }
        });
        return place(table, TicketSource.QR, null, items);
    }

    // The table row is locked, so two first orders cannot open two bills.
    private OrderTicket place(DiningTable table, TicketSource source, String waiterSub, List<TicketItem> items) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        TableSession session = sessions.findOpenByTable(table.id()).orElse(null);
        boolean opened = session == null;
        if (opened) {
            session = sessions.save(TableSession.open(table.id(), now));
        }
        OrderTicket ticket = tickets.save(OrderTicket.place(session.id(), table.id(), table.number(), source,
                waiterSub, items, now));
        events.ticketChanged(ticket);
        if (opened) events.sessionChanged(table.id());
        return ticket;
    }
}
