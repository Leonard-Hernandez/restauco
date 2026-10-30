package com.lsoftware.restauco.entity.tenant.gateway;

import com.lsoftware.restauco.entity.tenant.model.Tenant;

/**
 * Restaurant the current request belongs to (resolved from the X-Tenant header).
 */
public interface CurrentTenantGateway {
    Tenant current();
}
