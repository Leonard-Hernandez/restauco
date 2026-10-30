package com.lsoftware.restauco.usecase.floor.dto;

import com.lsoftware.restauco.entity.floor.model.DiningTable;
import com.lsoftware.restauco.entity.floor.model.TableSession;
import com.lsoftware.restauco.entity.order.model.OrderTicket;

import java.util.List;

/** A table with its open bill (if any) and every round ordered on it. */
public record TableDetail(
        DiningTable table,
        TableSession session,
        List<OrderTicket> tickets,
        long total
) {
    public static TableDetail of(DiningTable table, TableSession session, List<OrderTicket> tickets) {
        return new TableDetail(table, session, tickets, tickets.stream().mapToLong(OrderTicket::billable).sum());
    }
}
