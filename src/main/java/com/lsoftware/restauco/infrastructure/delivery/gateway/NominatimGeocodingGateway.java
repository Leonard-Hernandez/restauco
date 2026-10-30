package com.lsoftware.restauco.infrastructure.delivery.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lsoftware.restauco.entity.delivery.exception.GeocoderUnavailableException;
import com.lsoftware.restauco.entity.delivery.gateway.GeocodingGateway;
import com.lsoftware.restauco.entity.delivery.model.Place;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * OpenStreetMap's Nominatim: free and without a key, but with a fair-use policy (one request
 * per second, an identifying User-Agent, cache what you can). So calls are spaced out, and
 * answers kept for a while. Point it at a self-hosted Nominatim with restauco.geocoder.url.
 */
@Slf4j
@Component
public class NominatimGeocodingGateway implements GeocodingGateway {

    static final Duration SPACING = Duration.ofMillis(1100);
    static final int CACHE_SIZE = 2000;
    /** 1 degree of latitude in km */
    private static final double KM_PER_DEGREE = 111.32;

    private final RestClient http;
    private final Map<String, Object> cache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
            return size() > CACHE_SIZE;
        }
    };
    private long lastCall;

    public NominatimGeocodingGateway(RestClient.Builder builder,
                                     @Value("${restauco.geocoder.url:https://nominatim.openstreetmap.org}") String url,
                                     @Value("${restauco.geocoder.user-agent:restauco/1.0}") String userAgent) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(4)).build());
        factory.setReadTimeout(Duration.ofSeconds(6));
        this.http = builder.clone().baseUrl(url).requestFactory(factory)
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept-Language", "es")
                .build();
    }

    @Override
    public List<Place> search(String address, double nearLat, double nearLng, double radiusKm) {
        double dLat = radiusKm / KM_PER_DEGREE;
        double dLng = radiusKm / (KM_PER_DEGREE * Math.cos(Math.toRadians(nearLat)));
        String viewbox = String.format(Locale.ROOT, "%.5f,%.5f,%.5f,%.5f", nearLng - dLng, nearLat + dLat, nearLng + dLng, nearLat - dLat);
        String key = "s:" + address.toLowerCase(Locale.ROOT) + "@" + viewbox;
        return cached(key, () -> {
            Result[] results = call(() -> http.get()
                    .uri(u -> u.path("/search").queryParam("q", address).queryParam("format", "jsonv2")
                            .queryParam("addressdetails", 1).queryParam("limit", 8).queryParam("countrycodes", "co")
                            .queryParam("viewbox", viewbox).queryParam("bounded", 1).build())
                    .retrieve().body(Result[].class));
            return results == null ? List.<Place>of() : Arrays.stream(results).map(Result::toPlace).flatMap(Optional::stream).toList();
        });
    }

    @Override
    public Optional<Place> reverse(double lat, double lng) {
        String key = String.format(Locale.ROOT, "r:%.5f,%.5f", lat, lng);
        return cached(key, () -> {
            Result result = call(() -> http.get()
                    .uri(u -> u.path("/reverse").queryParam("lat", lat).queryParam("lon", lng).queryParam("format", "jsonv2")
                            .queryParam("addressdetails", 1).queryParam("zoom", 18).build())
                    .retrieve().body(Result.class));
            return result == null ? Optional.<Place>empty() : result.toPlace();
        });
    }

    @SuppressWarnings("unchecked")
    private <T> T cached(String key, Supplier<T> load) {
        synchronized (cache) {
            Object hit = cache.get(key);
            if (hit != null) return (T) hit;
        }
        T value = load.get();
        synchronized (cache) {
            cache.put(key, value);
        }
        return value;
    }

    // One request at a time, at least SPACING apart (the public server's policy)
    private synchronized <T> T call(Supplier<T> request) {
        long wait = lastCall + SPACING.toMillis() - System.currentTimeMillis();
        try {
            if (wait > 0) Thread.sleep(wait);
            return request.get();
        } catch (RestClientException e) {
            log.warn("Geocoder failed: {}", e.getMostSpecificCause().toString());
            throw new GeocoderUnavailableException();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeocoderUnavailableException();
        } finally {
            lastCall = System.currentTimeMillis();
        }
    }

    record Result(String lat, String lon, Address address) {
        Optional<Place> toPlace() {
            if (lat == null || lon == null || address == null) return Optional.empty();
            return Optional.of(new Place(Double.parseDouble(lat), Double.parseDouble(lon), address.street(),
                    address.houseNumber(), address.area(), address.locality()));
        }
    }

    record Address(
            String road,
            String pedestrian,
            @JsonProperty("house_number") String houseNumber,
            String neighbourhood,
            String suburb,
            String quarter,
            String city,
            String town,
            String village
    ) {
        String street() {
            return road != null ? road : pedestrian;
        }

        String area() {
            return neighbourhood != null ? neighbourhood : suburb != null ? suburb : quarter;
        }

        String locality() {
            return city != null ? city : town != null ? town : village;
        }
    }
}
