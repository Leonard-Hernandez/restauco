package com.lsoftware.restauco.infrastructure.member;

import com.lsoftware.restauco.infrastructure.config.db.repository.JpaTenantMemberRepository;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaTenantRepository;
import com.lsoftware.restauco.infrastructure.config.db.schema.TenantMemberEntity;
import com.lsoftware.restauco.infrastructure.config.tenant.TenantContext;
import com.lsoftware.restauco.infrastructure.config.db.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Invites the first OWNER of a restaurant (restauco.bootstrap.owner-email / tenant), so someone
 * can enter the panel. Idempotent; the invitation links on that person's first login.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OwnerBootstrap implements ApplicationRunner {

    private final JpaTenantRepository tenants;
    private final JpaTenantMemberRepository members;
    private final TenantMapper tenantMapper;
    private final TransactionTemplate tx;

    @Value("${restauco.bootstrap.owner-email:}")
    private String ownerEmail;

    @Value("${restauco.bootstrap.tenant:toches-arepas}")
    private String tenantSlug;

    @Override
    public void run(ApplicationArguments args) {
        if (ownerEmail == null || ownerEmail.isBlank()) return;
        String email = ownerEmail.strip().toLowerCase();
        TenantContext.asSystem(() -> tenants.findBySlug(tenantSlug)).map(tenantMapper::toDomain).ifPresentOrElse(
                tenant -> TenantContext.as(tenant, () -> tx.execute(status -> {
                    if (!members.existsByEmailIgnoreCase(email)) {
                        members.save(TenantMemberEntity.builder().email(email).role("OWNER").build());
                        log.info("Invited {} as OWNER of {}", email, tenant.slug());
                    }
                    return null;
                })),
                () -> log.warn("Bootstrap tenant {} not found", tenantSlug));
    }
}
