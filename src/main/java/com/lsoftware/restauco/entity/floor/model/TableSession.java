package com.lsoftware.restauco.entity.floor.model;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The bill of a table, from its first order until it is paid. A delivery is a bill with no
 * table (tableId null) that also charges the delivery fee.
 */
public record TableSession(
        Long id,
        Long tableId,
        SessionStatus status,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt,
        String closedBy,
        Long paymentMethodId,
        String paymentMethodName,
        Long total,
        List<Payment> payments,
        /** Whole pesos, added to the rounds when charging; 0 for tables. */
        long deliveryFee
) {
    public TableSession {
        payments = payments == null ? List.of() : List.copyOf(payments);
    }

    public TableSession(Long id, Long tableId, SessionStatus status, OffsetDateTime openedAt, OffsetDateTime closedAt,
                        String closedBy, Long paymentMethodId, String paymentMethodName, Long total, List<Payment> payments) {
        this(id, tableId, status, openedAt, closedAt, closedBy, paymentMethodId, paymentMethodName, total, payments, 0);
    }

    public static TableSession open(Long tableId, OffsetDateTime now) {
        return new TableSession(null, tableId, SessionStatus.OPEN, now, null, null, null, null, null, List.of(), 0);
    }

    public static TableSession openDelivery(long fee, OffsetDateTime now) {
        return new TableSession(null, null, SessionStatus.OPEN, now, null, null, null, null, null, List.of(), fee);
    }

    public boolean isOpen() {
        return status == SessionStatus.OPEN;
    }

    public boolean isDelivery() {
        return tableId == null;
    }

    /**
     * Paid with one or more methods. The largest part is the main method; the name reads
     * "Tarjeta + Efectivo" so lists that show one method still tell the whole story.
     */
    public TableSession close(List<Payment> paidWith, long total, String closedBy, OffsetDateTime now) {
        Payment main = paidWith.stream().max(Comparator.comparingLong(Payment::amount)).orElseThrow();
        String summary = paidWith.stream().map(Payment::name).collect(Collectors.joining(" + "));
        return new TableSession(id, tableId, SessionStatus.CLOSED, openedAt, now, closedBy,
                main.methodId(), summary.length() > 40 ? main.name() : summary, total, paidWith, deliveryFee);
    }

    /** Nothing to charge: the only round was rejected. */
    public TableSession cancel(String closedBy, OffsetDateTime now) {
        return new TableSession(id, tableId, SessionStatus.CANCELLED, openedAt, now, closedBy, null, null, 0L,
                List.of(), deliveryFee);
    }
}
