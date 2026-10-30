package com.lsoftware.restauco.infrastructure.config.tenant;

import com.lsoftware.restauco.entity.tenant.model.Tenant;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Feeds Hibernate's @TenantId discriminator. Without a tenant (and outside system mode) it
 * resolves to an id that matches no row, so a missing context never leaks other tenants' data.
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {

    static final Long SYSTEM = 0L;
    static final Long NONE = -1L;

    @Override
    public Long resolveCurrentTenantIdentifier() {
        if (TenantContext.isSystem()) return SYSTEM;
        return TenantContext.current().map(Tenant::id).orElse(NONE);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public boolean isRoot(Long tenantId) {
        return SYSTEM.equals(tenantId);
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
