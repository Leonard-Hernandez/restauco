package com.lsoftware.restauco.entity.order.model;

import java.util.List;

/**
 * A line of a ticket, priced and named when ordered: later menu edits never change it.
 * unitPrice includes the modifiers.
 */
public record TicketItem(
        Long dishId,
        String name,
        long unitPrice,
        int qty,
        String note,
        /** Ingredients the guest asked to leave out. */
        List<String> removed,
        List<TicketItemModifier> modifiers
) {
    public TicketItem {
        removed = removed != null ? List.copyOf(removed) : List.of();
        modifiers = modifiers != null ? List.copyOf(modifiers) : List.of();
    }

    public long lineTotal() {
        return unitPrice * qty;
    }
}
