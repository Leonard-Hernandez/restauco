package com.lsoftware.restauco.infrastructure.member.dto;

import com.lsoftware.restauco.entity.member.model.MemberRole;
import com.lsoftware.restauco.entity.member.model.TenantMember;

public record StaffResponse(String email, String name, MemberRole role) {
    public static StaffResponse from(TenantMember m) {
        return new StaffResponse(m.email(), m.name(), m.role());
    }
}
