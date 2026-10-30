package com.lsoftware.restauco.entity.delivery.model;

/**
 * A point the map knows: an address or a street, with its neighbourhood. road is null when
 * the point is not on or next to a street (a park, a river).
 */
public record Place(
        double lat,
        double lng,
        String road,
        String houseNumber,
        String neighbourhood,
        String city
) {
    /** "Calle 5B 36-20, San Fernando, Cali" */
    public String label() {
        StringBuilder sb = new StringBuilder();
        if (road != null) sb.append(road);
        if (houseNumber != null) sb.append(sb.isEmpty() ? "" : " ").append(houseNumber);
        for (String part : new String[]{neighbourhood, city}) {
            if (part != null && !part.isBlank()) sb.append(sb.isEmpty() ? "" : ", ").append(part);
        }
        String label = sb.isEmpty() ? "%.5f, %.5f".formatted(lat, lng) : sb.toString();
        return label.length() > 255 ? label.substring(0, 255) : label;
    }
}
