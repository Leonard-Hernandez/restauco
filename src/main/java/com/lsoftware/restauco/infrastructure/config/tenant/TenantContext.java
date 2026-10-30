package com.lsoftware.restauco.infrastructure.config.tenant;

import com.lsoftware.restauco.entity.tenant.model.Tenant;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Tenant of the current thread. Set by {@link TenantFilter} for HTTP requests, or explicitly
 * switched to system mode by background jobs that work across tenants.
 */
public final class TenantContext {

    private static final ThreadLocal<Tenant> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SYSTEM = new ThreadLocal<>();

    private TenantContext() {}

    public static Optional<Tenant> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static boolean isSystem() {
        return Boolean.TRUE.equals(SYSTEM.get());
    }

    static void set(Tenant tenant) {
        CURRENT.set(tenant);
    }

    static void clear() {
        CURRENT.remove();
        SYSTEM.remove();
    }

    /** Runs with tenant filtering disabled (sees every tenant's rows). */
    public static <T> T asSystem(Supplier<T> action) {
        SYSTEM.set(true);
        try {
            return action.get();
        } finally {
            SYSTEM.remove();
        }
    }

    /** Runs as a given tenant, for jobs that act on behalf of one restaurant. */
    public static <T> T as(Tenant tenant, Supplier<T> action) {
        Tenant previous = CURRENT.get();
        CURRENT.set(tenant);
        try {
            return action.get();
        } finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }
}
