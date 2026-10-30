package com.lsoftware.restauco.infrastructure.order.dto;

import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.usecase.order.dto.GuestTable;

import java.util.List;

public record GuestTableResponse(
        int number,
        String name,
        boolean active,
        List<TicketResponse> tickets,
        long total
) {
    public static GuestTableResponse from(GuestTable g) {
        return new GuestTableResponse(g.table().number(), g.table().displayName(), g.table().active(),
                g.tickets().stream().map(TicketResponse::from).toList(),
                g.tickets().stream().mapToLong(OrderTicket::billable).sum());
    }
}
