package com.lsoftware.restauco.entity.order.model;

public record TicketItemModifier(
        String group,
        String name,
        long price
) {}
