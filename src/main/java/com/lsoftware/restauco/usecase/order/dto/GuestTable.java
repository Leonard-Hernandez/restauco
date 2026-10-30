package com.lsoftware.restauco.usecase.order.dto;

import com.lsoftware.restauco.entity.floor.model.DiningTable;
import com.lsoftware.restauco.entity.order.model.OrderTicket;

import java.util.List;

/** What a guest who scanned the table QR may see: the table and its current rounds. */
public record GuestTable(
        DiningTable table,
        List<OrderTicket> tickets
) {}
