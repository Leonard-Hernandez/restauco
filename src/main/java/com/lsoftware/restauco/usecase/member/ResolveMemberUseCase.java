package com.lsoftware.restauco.usecase.member;

import com.lsoftware.restauco.entity.identity.gateway.IdentityGateway;
import com.lsoftware.restauco.entity.identity.model.UserIdentity;
import com.lsoftware.restauco.entity.member.gateway.MemberGateway;
import com.lsoftware.restauco.entity.member.model.TenantMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Staff membership of the current restaurant. Members are invited by email, so the first
 * visit links the invitation to the authco subject (only for verified emails).
 */
@Component
@RequiredArgsConstructor
public class ResolveMemberUseCase {

    private final MemberGateway members;
    private final IdentityGateway identity;

    @Transactional
    public Optional<TenantMember> execute(String sub, String accessToken) {
        Optional<TenantMember> linked = members.findBySub(sub);
        if (linked.isPresent()) return linked;

        UserIdentity user = identity.userInfo(accessToken);
        if (!user.emailVerified() || user.email() == null) return Optional.empty();

        return members.findUnlinkedByEmail(user.email().toLowerCase())
                .map(invite -> members.save(invite.linkedTo(sub, user.name())));
    }
}
