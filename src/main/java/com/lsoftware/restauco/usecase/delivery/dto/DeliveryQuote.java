package com.lsoftware.restauco.usecase.delivery.dto;

import com.lsoftware.restauco.entity.delivery.model.Place;

/** A place the customer could mean, how far it is and whether the restaurant goes there. */
public record DeliveryQuote(
        Place place,
        int distanceMeters,
        boolean inZone,
        long fee
) {}
