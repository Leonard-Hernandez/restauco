package com.lsoftware.restauco.infrastructure.config.db.mapper;

import com.lsoftware.restauco.entity.member.model.MemberRole;
import com.lsoftware.restauco.entity.member.model.TenantMember;
import com.lsoftware.restauco.infrastructure.config.db.schema.TenantMemberEntity;
import org.springframework.stereotype.Component;

@Component
public class TenantMemberMapper {

    public TenantMember toDomain(TenantMemberEntity e) {
        return new TenantMember(e.getId(), e.getEmail(), e.getAuthcoSub(), e.getName(), MemberRole.valueOf(e.getRole()));
    }

    public TenantMemberEntity toEntity(TenantMember m) {
        return TenantMemberEntity.builder()
                .id(m.id())
                .email(m.email())
                .authcoSub(m.authcoSub())
                .name(m.name())
                .role(m.role().name())
                .build();
    }
}
