package com.lsoftware.restauco.entity.tenant.exception;

import com.lsoftware.restauco.entity.exception.NotFoundException;

public class TenantNotFoundException extends NotFoundException {
    public TenantNotFoundException() {
        super("TENANT_NOT_FOUND", "Restaurant not found");
    }
}
