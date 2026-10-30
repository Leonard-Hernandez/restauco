package com.lsoftware.restauco.entity.tenant.model;

import com.lsoftware.restauco.entity.delivery.model.DeliverySettings;

import java.time.ZoneId;
import java.util.Map;

public record Tenant(
        Long id,
        String slug,
        String name,
        ZoneId timezone,
        String contactPhone,
        String address,
        /** Theme overrides and public profile (tagline, hours, phones...) read by the site. */
        Map<String, Object> branding,
        DeliverySettings delivery
) {
    public Tenant {
        delivery = delivery != null ? delivery : DeliverySettings.OFF;
    }

    public Tenant(Long id, String slug, String name, ZoneId timezone, String contactPhone, String address,
                  Map<String, Object> branding) {
        this(id, slug, name, timezone, contactPhone, address, branding, DeliverySettings.OFF);
    }
}
