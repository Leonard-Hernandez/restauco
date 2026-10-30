package com.lsoftware.restauco.infrastructure.tenant.dto;

import com.lsoftware.restauco.entity.tenant.model.Tenant;

import java.util.Map;

public record TenantResponse(
        String slug,
        String name,
        String timezone,
        String contactPhone,
        String address,
        Map<String, Object> branding
) {
    public static TenantResponse from(Tenant t) {
        return new TenantResponse(t.slug(), t.name(), t.timezone().getId(), t.contactPhone(),
                t.address(), t.branding());
    }
}
