package com.lsoftware.restauco.infrastructure.member.gateway;

import com.lsoftware.restauco.entity.member.gateway.MemberGateway;
import com.lsoftware.restauco.entity.member.model.TenantMember;
import com.lsoftware.restauco.infrastructure.config.db.mapper.TenantMemberMapper;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaTenantMemberRepository;
import com.lsoftware.restauco.infrastructure.config.db.schema.TenantMemberEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MemberJpaGateway implements MemberGateway {

    private final JpaTenantMemberRepository repo;
    private final TenantMemberMapper mapper;

    @Override
    public TenantMember save(TenantMember member) {
        TenantMemberEntity entity = mapper.toEntity(member);
        if (member.id() != null) {
            TenantMemberEntity existing = repo.findOneById(member.id()).orElseThrow();
            entity.setTenantId(existing.getTenantId());
            entity.setCreatedAt(existing.getCreatedAt());
        }
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<TenantMember> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<TenantMember> findBySub(String authcoSub) {
        return repo.findByAuthcoSub(authcoSub).map(mapper::toDomain);
    }

    @Override
    public Optional<TenantMember> findUnlinkedByEmail(String email) {
        return repo.findByEmailIgnoreCaseAndAuthcoSubIsNull(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repo.existsByEmailIgnoreCase(email);
    }

    @Override
    public List<TenantMember> findAll() {
        return repo.findAllByOrderByCreatedAtAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repo.findOneById(id).ifPresent(repo::delete);
    }
}
