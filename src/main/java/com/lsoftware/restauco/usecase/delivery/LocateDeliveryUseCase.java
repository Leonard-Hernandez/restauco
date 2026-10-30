package com.lsoftware.restauco.usecase.delivery;

import com.lsoftware.restauco.entity.delivery.exception.DeliveryClosedException;
import com.lsoftware.restauco.entity.delivery.exception.InvalidDeliveryException;
import com.lsoftware.restauco.entity.delivery.exception.PlaceNotFoundException;
import com.lsoftware.restauco.entity.delivery.gateway.GeocodingGateway;
import com.lsoftware.restauco.entity.delivery.model.DeliverySettings;
import com.lsoftware.restauco.entity.delivery.model.Place;
import com.lsoftware.restauco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.restauco.usecase.delivery.dto.DeliveryQuote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Finds where the customer lives on the map, before they order: from what they wrote, or
 * from a pin (their GPS, or where they dragged it). Every place comes with whether the
 * restaurant delivers there.
 */
@Component
@RequiredArgsConstructor
public class LocateDeliveryUseCase {

    static final int MAX_RESULTS = 5;
    /** Look a bit past the zone, so "too far" is an answer and not "not found" */
    static final double SEARCH_MARGIN = 1.5;

    private final GeocodingGateway geocoder;
    private final CurrentTenantGateway tenants;

    public List<DeliveryQuote> search(String address) {
        DeliverySettings settings = settings();
        String text = AddressText.forSearch(address);
        if (text.length() < 4) {
            throw new InvalidDeliveryException("ADDRESS_REQUIRED", "Write the street and number");
        }
        return geocoder.search(text, settings.lat(), settings.lng(), settings.radiusKm() * SEARCH_MARGIN).stream()
                .filter(p -> p.road() != null)
                .map(p -> quote(settings, p))
                .sorted(Comparator.comparing((DeliveryQuote q) -> !q.inZone()).thenComparingInt(DeliveryQuote::distanceMeters))
                .limit(MAX_RESULTS)
                .toList();
    }

    /** What is at the pin. The point stays where the customer put it; the map gives its name. */
    public DeliveryQuote at(double lat, double lng) {
        DeliverySettings settings = settings();
        return quote(settings, verify(lat, lng));
    }

    /** The street address at a point, or PlaceNotFound when the map has none there. */
    Place verify(double lat, double lng) {
        if (!Double.isFinite(lat) || !Double.isFinite(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180) {
            throw new InvalidDeliveryException("INVALID_LOCATION", "Choose the place on the map");
        }
        Place found = geocoder.reverse(lat, lng).filter(p -> p.road() != null).orElseThrow(PlaceNotFoundException::new);
        return new Place(lat, lng, found.road(), found.houseNumber(), found.neighbourhood(), found.city());
    }

    private DeliverySettings settings() {
        DeliverySettings settings = tenants.current().delivery();
        if (!settings.open()) throw new DeliveryClosedException();
        return settings;
    }

    private static DeliveryQuote quote(DeliverySettings settings, Place place) {
        int distance = settings.distanceTo(place.lat(), place.lng());
        return new DeliveryQuote(place, distance, settings.reaches(distance), settings.fee());
    }
}
