package com.lsoftware.restauco.usecase.floor;

import com.lsoftware.restauco.entity.floor.exception.InvalidPaymentsException;
import com.lsoftware.restauco.entity.floor.exception.PaymentMethodNotFoundException;
import com.lsoftware.restauco.entity.floor.exception.PaymentsDoNotMatchTotalException;
import com.lsoftware.restauco.entity.floor.exception.SessionHasActiveTicketsException;
import com.lsoftware.restauco.entity.floor.gateway.PaymentMethodGateway;
import com.lsoftware.restauco.entity.floor.gateway.SessionGateway;
import com.lsoftware.restauco.entity.floor.model.Payment;
import com.lsoftware.restauco.entity.floor.model.PaymentMethod;
import com.lsoftware.restauco.entity.floor.model.SessionStatus;
import com.lsoftware.restauco.entity.floor.model.TableSession;
import com.lsoftware.restauco.entity.order.gateway.OrderEventGateway;
import com.lsoftware.restauco.entity.order.gateway.TicketGateway;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.order.model.TicketItem;
import com.lsoftware.restauco.entity.order.model.TicketSource;
import com.lsoftware.restauco.entity.order.model.TicketStatus;
import com.lsoftware.restauco.usecase.floor.CloseSessionUseCase.PaymentPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CloseSessionUseCaseTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-20T23:00:00Z");
    private static final List<TicketItem> ITEMS = List.of(new TicketItem(1L, "Arepa Pelúa", 15000, 1, null, List.of(), List.of()));

    private final SessionGateway sessions = mock(SessionGateway.class);
    private final TicketGateway tickets = mock(TicketGateway.class);
    private final PaymentMethodGateway methods = mock(PaymentMethodGateway.class);
    private final OrderEventGateway events = mock(OrderEventGateway.class);
    private final CloseSessionUseCase useCase = new CloseSessionUseCase(sessions, tickets, methods, events,
            Clock.fixed(Instant.parse("2026-10-21T01:00:00Z"), ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        when(sessions.findById(5L)).thenReturn(Optional.of(
                new TableSession(5L, 7L, SessionStatus.OPEN, NOW, null, null, null, null, null, null)));
        when(methods.findById(1L)).thenReturn(Optional.of(new PaymentMethod(1L, "Efectivo", 0, true)));
        when(methods.findById(2L)).thenReturn(Optional.of(new PaymentMethod(2L, "Nequi", 2, true)));
        when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void chargesDeliveredRoundsOnly() {
        when(tickets.findBySession(5L)).thenReturn(List.of(
                ticket().moveTo(TicketStatus.READY, NOW).moveTo(TicketStatus.DELIVERED, NOW),
                ticket().cancel("repetido", NOW)));

        TableSession closed = useCase.execute(5L, 2L, "waiter-sub");

        assertThat(closed.status()).isEqualTo(SessionStatus.CLOSED);
        assertThat(closed.total()).isEqualTo(15000);
        assertThat(closed.paymentMethodName()).isEqualTo("Nequi");
        assertThat(closed.closedBy()).isEqualTo("waiter-sub");
        assertThat(closed.payments()).containsExactly(new Payment(2L, "Nequi", 15000));
        verify(events).sessionChanged(7L);
    }

    @Test
    void cannotCloseWhileTheKitchenIsStillWorking() {
        when(tickets.findBySession(5L)).thenReturn(List.of(ticket()));

        assertThatThrownBy(() -> useCase.execute(5L, 2L, "waiter-sub"))
                .isInstanceOf(SessionHasActiveTicketsException.class);
        verify(sessions, never()).save(any());
    }

    @Test
    void inactivePaymentMethodsAreRejected() {
        when(methods.findById(3L)).thenReturn(Optional.of(new PaymentMethod(3L, "Bitcoin", 9, false)));

        assertThatThrownBy(() -> useCase.execute(5L, 3L, "waiter-sub"))
                .isInstanceOf(PaymentMethodNotFoundException.class);
    }

    @Test
    void splitsTheBillAcrossMethods() {
        when(tickets.findBySession(5L)).thenReturn(List.of(delivered()));

        TableSession closed = useCase.execute(5L,
                List.of(new PaymentPart(2L, 10000L), new PaymentPart(1L, 5000L)), "waiter-sub");

        assertThat(closed.payments()).containsExactly(new Payment(2L, "Nequi", 10000), new Payment(1L, "Efectivo", 5000));
        assertThat(closed.paymentMethodId()).isEqualTo(2L);
        assertThat(closed.paymentMethodName()).isEqualTo("Nequi + Efectivo");
        assertThat(closed.total()).isEqualTo(15000);
    }

    @Test
    void splitMustAddUpToTheBill() {
        when(tickets.findBySession(5L)).thenReturn(List.of(delivered()));

        assertThatThrownBy(() -> useCase.execute(5L,
                List.of(new PaymentPart(2L, 10000L), new PaymentPart(1L, 2000L)), "waiter-sub"))
                .isInstanceOf(PaymentsDoNotMatchTotalException.class);
        verify(sessions, never()).save(any());
    }

    @Test
    void splitNeedsAnAmountForEachMethodAndNoRepeats() {
        when(tickets.findBySession(5L)).thenReturn(List.of(delivered()));

        assertThatThrownBy(() -> useCase.execute(5L,
                List.of(new PaymentPart(2L, null), new PaymentPart(1L, 5000L)), "waiter-sub"))
                .isInstanceOf(InvalidPaymentsException.class);
        assertThatThrownBy(() -> useCase.execute(5L,
                List.of(new PaymentPart(2L, 7500L), new PaymentPart(2L, 7500L)), "waiter-sub"))
                .isInstanceOf(InvalidPaymentsException.class);
    }

    private static OrderTicket delivered() {
        return ticket().moveTo(TicketStatus.READY, NOW).moveTo(TicketStatus.DELIVERED, NOW);
    }

    private static OrderTicket ticket() {
        return OrderTicket.place(5L, 7L, 4, TicketSource.WAITER, "waiter-sub", ITEMS, NOW);
    }
}
