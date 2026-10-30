package com.lsoftware.restauco.usecase.floor;

import com.lsoftware.restauco.entity.floor.exception.InvalidPaymentsException;
import com.lsoftware.restauco.entity.floor.exception.PaymentMethodNotFoundException;
import com.lsoftware.restauco.entity.floor.exception.PaymentsDoNotMatchTotalException;
import com.lsoftware.restauco.entity.floor.exception.SessionClosedException;
import com.lsoftware.restauco.entity.floor.exception.SessionHasActiveTicketsException;
import com.lsoftware.restauco.entity.floor.exception.SessionNotFoundException;
import com.lsoftware.restauco.entity.floor.gateway.PaymentMethodGateway;
import com.lsoftware.restauco.entity.floor.gateway.SessionGateway;
import com.lsoftware.restauco.entity.floor.model.Payment;
import com.lsoftware.restauco.entity.floor.model.PaymentMethod;
import com.lsoftware.restauco.entity.floor.model.TableSession;
import com.lsoftware.restauco.entity.order.gateway.OrderEventGateway;
import com.lsoftware.restauco.entity.order.gateway.TicketGateway;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Charges the bill with the method(s) the guest used and frees the table (a delivery adds its fee). */
@Component
@RequiredArgsConstructor
public class CloseSessionUseCase {

    static final int MAX_PARTS = 5;

    /** amount null means "the rest of the bill", allowed only when paying with one method. */
    public record PaymentPart(Long methodId, Long amount) {}

    private final SessionGateway sessions;
    private final TicketGateway tickets;
    private final PaymentMethodGateway paymentMethods;
    private final OrderEventGateway events;
    private final Clock clock;

    @Transactional
    public TableSession execute(Long sessionId, Long paymentMethodId, String closedBy) {
        return execute(sessionId, List.of(new PaymentPart(paymentMethodId, null)), closedBy);
    }

    @Transactional
    public TableSession execute(Long sessionId, List<PaymentPart> parts, String closedBy) {
        TableSession session = sessions.findById(sessionId).orElseThrow(SessionNotFoundException::new);
        if (!session.isOpen()) throw new SessionClosedException();

        List<OrderTicket> rounds = tickets.findBySession(sessionId);
        if (rounds.stream().anyMatch(t -> t.status().active())) throw new SessionHasActiveTicketsException();
        long total = rounds.stream().mapToLong(OrderTicket::billable).sum() + session.deliveryFee();

        List<Payment> paid = payments(parts, total);
        TableSession closed = sessions.save(session.close(paid, total, closedBy, OffsetDateTime.now(clock)));
        events.sessionChanged(session.tableId());
        return closed;
    }

    private List<Payment> payments(List<PaymentPart> parts, long total) {
        if (parts == null || parts.isEmpty()) throw new InvalidPaymentsException("Choose how the bill was paid");
        if (parts.size() > MAX_PARTS) throw new InvalidPaymentsException("Use at most " + MAX_PARTS + " payment methods");

        Set<Long> seen = new HashSet<>();
        List<Payment> paid = new ArrayList<>();
        for (PaymentPart part : parts) {
            PaymentMethod method = paymentMethods.findById(part.methodId())
                    .filter(PaymentMethod::active)
                    .orElseThrow(PaymentMethodNotFoundException::new);
            if (!seen.add(method.id())) throw new InvalidPaymentsException("Each payment method can appear once");

            long amount;
            if (part.amount() == null) {
                if (parts.size() > 1) throw new InvalidPaymentsException("Give the amount paid with each method");
                amount = total;
            } else {
                amount = part.amount();
                if (amount <= 0 && total > 0) throw new InvalidPaymentsException("Each payment must be more than zero");
            }
            paid.add(new Payment(method.id(), method.name(), amount));
        }

        long sum = paid.stream().mapToLong(Payment::amount).sum();
        if (sum != total) throw new PaymentsDoNotMatchTotalException(sum, total);
        return paid;
    }
}
