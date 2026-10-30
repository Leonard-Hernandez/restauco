package com.lsoftware.restauco.infrastructure.delivery.dto;

import com.lsoftware.restauco.entity.delivery.model.DeliverySettings;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Also the response: the owner's delivery settings as stored. */
public record DeliverySettingsRequest(
        boolean enabled,
        Double lat,
        Double lng,
        double radiusKm,
        @Min(0) @Max(1_000_000) long fee,
        @Min(0) @Max(10_000_000) long minOrder,
        /** Accepted for delivery; empty = every active method */
        @Size(max = 20) List<Long> paymentMethodIds
) {
    public DeliverySettings toDomain() {
        return new DeliverySettings(enabled, lat, lng, radiusKm, fee, minOrder, paymentMethodIds);
    }

    public static DeliverySettingsRequest from(DeliverySettings s) {
        return new DeliverySettingsRequest(s.enabled(), s.lat(), s.lng(), s.radiusKm(), s.fee(), s.minOrder(),
                s.paymentMethodIds());
    }
}
