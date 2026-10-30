package com.lsoftware.restauco.entity.delivery.gateway;

import com.lsoftware.restauco.entity.delivery.model.Place;

import java.util.List;
import java.util.Optional;

/**
 * The map service. Throws {@code GeocoderUnavailableException} when it cannot be reached:
 * that is not the same as "no such place".
 */
public interface GeocodingGateway {
    /** Places matching a written address, within radiusKm of a point, best first. */
    List<Place> search(String address, double nearLat, double nearLng, double radiusKm);
    /** What is at a point: the nearest address or street. */
    Optional<Place> reverse(double lat, double lng);
}
