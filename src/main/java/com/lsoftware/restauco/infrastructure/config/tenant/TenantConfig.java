package com.lsoftware.restauco.infrastructure.config.tenant;

import com.lsoftware.restauco.entity.tenant.gateway.TenantGateway;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantConfig {

    @Bean
    FilterRegistrationBean<TenantFilter> tenantFilter(TenantGateway tenants) {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>(new TenantFilter(tenants));
        registration.addUrlPatterns("/api/*");
        // Before the Spring Security chain
        registration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 10);
        return registration;
    }
}
