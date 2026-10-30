package com.lsoftware.restauco.infrastructure.member.dto;

import com.lsoftware.restauco.entity.member.model.MemberRole;
import com.lsoftware.restauco.entity.member.model.TenantMember;

/** linked = the person already signed in once with that email. */
public record MemberResponse(Long id, String email, String name, MemberRole role, boolean linked) {
    public static MemberResponse from(TenantMember m) {
        return new MemberResponse(m.id(), m.email(), m.name(), m.role(), m.authcoSub() != null);
    }
}
