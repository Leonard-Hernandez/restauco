package com.lsoftware.restauco.infrastructure.config.tenant;

import com.lsoftware.restauco.entity.tenant.exception.TenantNotFoundException;
import com.lsoftware.restauco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.restauco.entity.tenant.model.Tenant;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenantContextGateway implements CurrentTenantGateway {

    @Override
    public Tenant current() {
        return TenantContext.current().orElseThrow(TenantNotFoundException::new);
    }
}
