package com.lsoftware.restauco.entity.delivery.model;

import java.util.List;

/**
 * How the restaurant delivers: from where (lat/lng), how far, for how much (fee, whole pesos),
 * from what order total (minOrder) and how customers may pay (paymentMethodIds; empty = every
 * active method).
 */
public record DeliverySettings(
        boolean enabled,
        Double lat,
        Double lng,
        double radiusKm,
        long fee,
        long minOrder,
        List<Long> paymentMethodIds
) {
    public static final DeliverySettings OFF = new DeliverySettings(false, null, null, 0, 0, 0, List.of());

    public DeliverySettings {
        paymentMethodIds = paymentMethodIds != null ? List.copyOf(paymentMethodIds) : List.of();
    }

    public DeliverySettings(boolean enabled, Double lat, Double lng, double radiusKm, long fee, long minOrder) {
        this(enabled, lat, lng, radiusKm, fee, minOrder, List.of());
    }

    /** Whether a delivery may be paid this way (the method must also be active). */
    public boolean accepts(Long paymentMethodId) {
        return paymentMethodIds.isEmpty() || paymentMethodIds.contains(paymentMethodId);
    }

    private static final double EARTH_RADIUS_M = 6_371_000;

    /** Taking orders: switched on, and it knows where the restaurant is and how far it goes. */
    public boolean open() {
        return enabled && lat != null && lng != null && radiusKm > 0;
    }

    /** Straight-line distance from the restaurant (haversine), in meters. */
    public int distanceTo(double toLat, double toLng) {
        double dLat = Math.toRadians(toLat - lat);
        double dLng = Math.toRadians(toLng - lng);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(toLat)) * Math.pow(Math.sin(dLng / 2), 2);
        return (int) Math.round(2 * EARTH_RADIUS_M * Math.asin(Math.sqrt(a)));
    }

    public boolean reaches(int distanceMeters) {
        return distanceMeters <= radiusKm * 1000;
    }
}
