package com.lsoftware.restauco.infrastructure.delivery.dto;

import com.lsoftware.restauco.usecase.delivery.dto.DeliveryQuote;

public record QuoteResponse(
        double lat,
        double lng,
        String label,
        String road,
        String neighbourhood,
        int distanceMeters,
        boolean inZone,
        long fee
) {
    public static QuoteResponse from(DeliveryQuote q) {
        return new QuoteResponse(q.place().lat(), q.place().lng(), q.place().label(), q.place().road(),
                q.place().neighbourhood(), q.distanceMeters(), q.inZone(), q.fee());
    }
}
