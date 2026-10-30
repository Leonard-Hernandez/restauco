package com.lsoftware.restauco.infrastructure.config.tenant;

import com.lsoftware.restauco.entity.tenant.gateway.TenantGateway;
import com.lsoftware.restauco.entity.tenant.model.Tenant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Resolves the restaurant of every /api request from the X-Tenant header (its slug).
 * Registered before Spring Security so the panel authorization can check membership.
 */
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Tenant";

    private final TenantGateway tenants;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.startsWith("/api/") || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String slug = request.getHeader(HEADER);
        if (slug == null || slug.isBlank()) {
            error(response, HttpStatus.BAD_REQUEST, "TENANT_REQUIRED", "Missing X-Tenant header");
            return;
        }
        Optional<Tenant> tenant = TenantContext.asSystem(() -> tenants.findBySlug(slug.strip().toLowerCase()));
        if (tenant.isEmpty()) {
            error(response, HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Restaurant not found");
            return;
        }
        TenantContext.set(tenant.get());
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private static void error(HttpServletResponse response, HttpStatus status, String code, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":\"%s\",\"message\":\"%s\"}".formatted(code, message));
    }
}
