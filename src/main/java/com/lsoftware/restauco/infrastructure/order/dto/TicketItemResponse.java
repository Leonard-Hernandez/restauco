package com.lsoftware.restauco.infrastructure.order.dto;

import com.lsoftware.restauco.entity.order.model.TicketItem;
import com.lsoftware.restauco.entity.order.model.TicketItemModifier;

import java.util.List;

public record TicketItemResponse(
        Long dishId,
        String name,
        long unitPrice,
        int qty,
        long lineTotal,
        String note,
        List<String> removed,
        List<TicketItemModifier> modifiers
) {
    public static TicketItemResponse from(TicketItem i) {
        return new TicketItemResponse(i.dishId(), i.name(), i.unitPrice(), i.qty(), i.lineTotal(), i.note(),
                i.removed(), i.modifiers());
    }
}
