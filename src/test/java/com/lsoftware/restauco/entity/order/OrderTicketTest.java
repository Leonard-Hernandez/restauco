package com.lsoftware.restauco.entity.order;

import com.lsoftware.restauco.entity.order.exception.InvalidTicketStateException;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.order.model.TicketItem;
import com.lsoftware.restauco.entity.order.model.TicketSource;
import com.lsoftware.restauco.entity.order.model.TicketStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTicketTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-20T23:00:00Z");
    private static final List<TicketItem> ITEMS = List.of(
            new TicketItem(1L, "Arepa Sifrina", 15000, 2, null, List.of(), List.of()),
            new TicketItem(2L, "Gaseosa", 4000, 1, null, List.of(), List.of()));

    @Test
    void waiterOrdersGoStraightToTheKitchen() {
        OrderTicket ticket = OrderTicket.place(1L, 1L, 3, TicketSource.WAITER, "sub", ITEMS, NOW);

        assertThat(ticket.status()).isEqualTo(TicketStatus.SENT);
        assertThat(ticket.sentAt()).isEqualTo(NOW);
        assertThat(ticket.total()).isEqualTo(34000);
    }

    @Test
    void qrOrdersWaitForTheWaiter() {
        OrderTicket ticket = OrderTicket.place(1L, 1L, 3, TicketSource.QR, null, ITEMS, NOW);

        assertThat(ticket.status()).isEqualTo(TicketStatus.PENDING_APPROVAL);
        assertThat(ticket.sentAt()).isNull();
    }

    @Test
    void fullLifecycleStampsEachStep() {
        OrderTicket ticket = OrderTicket.place(1L, 1L, 3, TicketSource.QR, null, ITEMS, NOW)
                .moveTo(TicketStatus.SENT, NOW.plusMinutes(1))
                .moveTo(TicketStatus.PREPARING, NOW.plusMinutes(2))
                .moveTo(TicketStatus.READY, NOW.plusMinutes(10))
                .moveTo(TicketStatus.DELIVERED, NOW.plusMinutes(11));

        assertThat(ticket.status()).isEqualTo(TicketStatus.DELIVERED);
        assertThat(ticket.sentAt()).isEqualTo(NOW.plusMinutes(1));
        assertThat(ticket.startedAt()).isEqualTo(NOW.plusMinutes(2));
        assertThat(ticket.readyAt()).isEqualTo(NOW.plusMinutes(10));
        assertThat(ticket.deliveredAt()).isEqualTo(NOW.plusMinutes(11));
    }

    @Test
    void cannotCancelOnceTheKitchenStarted() {
        OrderTicket preparing = OrderTicket.place(1L, 1L, 3, TicketSource.WAITER, "sub", ITEMS, NOW)
                .moveTo(TicketStatus.PREPARING, NOW);

        assertThatThrownBy(() -> preparing.cancel("cliente se fue", NOW))
                .isInstanceOf(InvalidTicketStateException.class);
    }

    @Test
    void cannotSkipToDelivered() {
        OrderTicket sent = OrderTicket.place(1L, 1L, 3, TicketSource.WAITER, "sub", ITEMS, NOW);

        assertThatThrownBy(() -> sent.moveTo(TicketStatus.DELIVERED, NOW))
                .isInstanceOf(InvalidTicketStateException.class);
    }

    @Test
    void cancelledTicketsAreNotCharged() {
        OrderTicket cancelled = OrderTicket.place(1L, 1L, 3, TicketSource.QR, null, ITEMS, NOW)
                .cancel("  pedido repetido ", NOW);

        assertThat(cancelled.billable()).isZero();
        assertThat(cancelled.cancelReason()).isEqualTo("pedido repetido");
        assertThat(cancelled.cancelledAt()).isEqualTo(NOW);
    }
}
