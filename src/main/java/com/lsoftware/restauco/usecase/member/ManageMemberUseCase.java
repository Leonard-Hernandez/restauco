package com.lsoftware.restauco.usecase.member;

import com.lsoftware.restauco.entity.member.exception.MemberAlreadyInvitedException;
import com.lsoftware.restauco.entity.member.exception.MemberNotFoundException;
import com.lsoftware.restauco.entity.member.exception.SelfChangeException;
import com.lsoftware.restauco.entity.member.gateway.MemberGateway;
import com.lsoftware.restauco.entity.member.model.MemberRole;
import com.lsoftware.restauco.entity.member.model.TenantMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** The restaurant team. People are invited by Google email and linked on their first login. */
@Component
@RequiredArgsConstructor
public class ManageMemberUseCase {

    private final MemberGateway members;

    @Transactional(readOnly = true)
    public List<TenantMember> list() {
        return members.findAll();
    }

    @Transactional
    public TenantMember invite(String email, MemberRole role) {
        String clean = email.strip().toLowerCase(Locale.ROOT);
        if (members.existsByEmail(clean)) throw new MemberAlreadyInvitedException();
        return members.save(new TenantMember(null, clean, null, null, role));
    }

    @Transactional
    public TenantMember changeRole(Long id, MemberRole role, String actingSub) {
        TenantMember member = members.findById(id).orElseThrow(MemberNotFoundException::new);
        if (actingSub.equals(member.authcoSub()) && role != MemberRole.OWNER) throw new SelfChangeException();
        return members.save(member.withRole(role));
    }

    @Transactional
    public void remove(Long id, String actingSub) {
        TenantMember member = members.findById(id).orElseThrow(MemberNotFoundException::new);
        if (actingSub.equals(member.authcoSub())) throw new SelfChangeException();
        members.deleteById(id);
    }
}
