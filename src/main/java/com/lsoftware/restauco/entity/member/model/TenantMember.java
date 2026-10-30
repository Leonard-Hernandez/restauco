package com.lsoftware.restauco.entity.member.model;

public record TenantMember(
        Long id,
        String email,
        String authcoSub,
        String name,
        MemberRole role
) {
    public TenantMember linkedTo(String sub, String displayName) {
        return new TenantMember(id, email, sub, displayName != null ? displayName : name, role);
    }

    public TenantMember withRole(MemberRole newRole) {
        return new TenantMember(id, email, authcoSub, name, newRole);
    }
}
