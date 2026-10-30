package com.lsoftware.restauco.usecase.delivery.dto;

import com.lsoftware.restauco.usecase.order.dto.OrderLine;

import java.util.List;

/** What the customer sends: contact, the address they wrote, the pin they confirmed, how they pay. */
public record DeliveryCommand(
        String customerName,
        String phone,
        String address,
        String addressDetail,
        double lat,
        double lng,
        Long paymentMethodId,
        Long cashFor,
        List<OrderLine> lines
) {}
