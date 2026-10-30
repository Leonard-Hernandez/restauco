package com.lsoftware.restauco.infrastructure.config.security;

import com.lsoftware.restauco.entity.member.model.MemberRole;
import com.lsoftware.restauco.entity.member.model.TenantMember;
import com.lsoftware.restauco.usecase.member.ResolveMemberUseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * /api/panel/** is only for staff of the restaurant named in X-Tenant, and each area for the
 * roles that work there (the owner can enter every area).
 */
@Component
@RequiredArgsConstructor
public class PanelAuthorizationManager {

    /** The resolved member, for controllers that need it. */
    public static final String MEMBER_ATTRIBUTE = "restauco.panel.member";

    private final ResolveMemberUseCase resolveMember;

    /** Any staff member. */
    public AuthorizationManager<RequestAuthorizationContext> member() {
        return roles(EnumSet.allOf(MemberRole.class));
    }

    /** Staff with one of these roles, or the owner. */
    public AuthorizationManager<RequestAuthorizationContext> roles(MemberRole... roles) {
        Set<MemberRole> allowed = EnumSet.of(MemberRole.OWNER, roles);
        return roles(allowed);
    }

    private AuthorizationManager<RequestAuthorizationContext> roles(Set<MemberRole> allowed) {
        return (authentication, context) -> {
            Optional<TenantMember> member = resolve(authentication.get(), context.getRequest());
            return new AuthorizationDecision(member.map(m -> allowed.contains(m.role())).orElse(false));
        };
    }

    private Optional<TenantMember> resolve(Authentication authentication, HttpServletRequest request) {
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            return Optional.empty();
        }
        if (request.getAttribute(MEMBER_ATTRIBUTE) instanceof TenantMember cached) {
            return Optional.of(cached);
        }
        Optional<TenantMember> member = resolveMember.execute(jwt.getToken().getSubject(), jwt.getToken().getTokenValue());
        member.ifPresent(m -> request.setAttribute(MEMBER_ATTRIBUTE, m));
        return member;
    }
}
