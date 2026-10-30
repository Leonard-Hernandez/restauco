package com.lsoftware.restauco.usecase.order;

import com.lsoftware.restauco.entity.floor.gateway.SessionGateway;
import com.lsoftware.restauco.entity.floor.gateway.TableGateway;
import com.lsoftware.restauco.entity.floor.model.DiningTable;
import com.lsoftware.restauco.entity.floor.model.SessionStatus;
import com.lsoftware.restauco.entity.floor.model.TableSession;
import com.lsoftware.restauco.entity.menu.gateway.DishGateway;
import com.lsoftware.restauco.entity.order.exception.TooManyPendingOrdersException;
import com.lsoftware.restauco.entity.order.gateway.OrderEventGateway;
import com.lsoftware.restauco.entity.order.gateway.TicketGateway;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.order.model.TicketStatus;
import com.lsoftware.restauco.usecase.order.dto.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlaceOrderUseCaseTest {

    private static final UUID TOKEN = UUID.randomUUID();
    private static final DiningTable TABLE = new DiningTable(7L, 4, null, TOKEN, true);
    private static final List<OrderLine> LINES = List.of(new OrderLine(2L, 1, null, null, null));

    private final TableGateway tables = mock(TableGateway.class);
    private final SessionGateway sessions = mock(SessionGateway.class);
    private final TicketGateway tickets = mock(TicketGateway.class);
    private final DishGateway dishes = mock(DishGateway.class);
    private final OrderEventGateway events = mock(OrderEventGateway.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-20T23:00:00Z"), ZoneOffset.UTC);
    private final PlaceOrderUseCase useCase = new PlaceOrderUseCase(tables, sessions, tickets, new OrderPricer(dishes),
            events, clock, 2);

    @BeforeEach
    void setUp() {
        when(tables.lockById(7L)).thenReturn(Optional.of(TABLE));
        when(tables.findByQrToken(TOKEN)).thenReturn(Optional.of(TABLE));
        when(dishes.findAllById(any())).thenReturn(List.of(MenuFixtures.SIFRINA));
        when(sessions.save(any())).thenAnswer(inv -> {
            TableSession s = inv.getArgument(0);
            return new TableSession(50L, s.tableId(), s.status(), s.openedAt(), null, null, null, null, null, null);
        });
        when(tickets.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void firstWaiterRoundOpensTheBillAndGoesToTheKitchen() {
        when(sessions.findOpenByTable(7L)).thenReturn(Optional.empty());

        OrderTicket ticket = useCase.byWaiter(7L, LINES, "waiter-sub");

        assertThat(ticket.status()).isEqualTo(TicketStatus.SENT);
        assertThat(ticket.sessionId()).isEqualTo(50L);
        assertThat(ticket.tableNumber()).isEqualTo(4);
        assertThat(ticket.total()).isEqualTo(15000);
        verify(events).ticketChanged(ticket);
        verify(events).sessionChanged(7L);
    }

    @Test
    void laterRoundsReuseTheOpenBill() {
        TableSession open = new TableSession(50L, 7L, SessionStatus.OPEN, OffsetDateTime.now(clock), null, null, null, null, null, null);
        when(sessions.findOpenByTable(7L)).thenReturn(Optional.of(open));

        useCase.byWaiter(7L, LINES, "waiter-sub");

        verify(sessions, never()).save(any());
        verify(events, never()).sessionChanged(any());
    }

    @Test
    void guestOrdersWaitForApproval() {
        when(sessions.findOpenByTable(7L)).thenReturn(Optional.empty());

        OrderTicket ticket = useCase.byGuest(TOKEN, LINES);

        assertThat(ticket.status()).isEqualTo(TicketStatus.PENDING_APPROVAL);
        assertThat(ticket.waiterSub()).isNull();
    }

    @Test
    void aTableCannotPileUpUnapprovedOrders() {
        TableSession open = new TableSession(50L, 7L, SessionStatus.OPEN, OffsetDateTime.now(clock), null, null, null, null, null, null);
        when(sessions.findOpenByTable(7L)).thenReturn(Optional.of(open));
        when(tickets.countBySessionAndStatus(50L, TicketStatus.PENDING_APPROVAL)).thenReturn(2L);

        assertThatThrownBy(() -> useCase.byGuest(TOKEN, LINES)).isInstanceOf(TooManyPendingOrdersException.class);
        verify(tickets, never()).save(any());
    }
}
