package com.lsoftware.restauco.entity.delivery.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Who ordered for delivery and where to take it. The order itself is a bill with no table
 * (sessionId): same rounds, kitchen and payment as a table.
 * <p>
 * address is what the customer wrote; lat/lng the pin they confirmed on the map; placeLabel
 * what the map says is there; matchesAddress whether that street is the one they wrote.
 */
public record DeliveryOrder(
        Long id,
        Long sessionId,
        /** Lets the customer follow the order; never shown to anyone else. */
        UUID token,
        String customerName,
        String phone,
        String address,
        /** Apartment, tower, how to find the door */
        String addressDetail,
        double lat,
        double lng,
        String placeLabel,
        boolean matchesAddress,
        int distanceMeters,
        Long paymentMethodId,
        String paymentMethodName,
        /** Paying cash with this bill, so the courier brings change */
        Long cashFor,
        OffsetDateTime createdAt
) {}
