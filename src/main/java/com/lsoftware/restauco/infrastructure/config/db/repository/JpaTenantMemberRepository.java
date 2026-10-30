package com.lsoftware.restauco.infrastructure.config.db.repository;

import com.lsoftware.restauco.infrastructure.config.db.schema.TenantMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaTenantMemberRepository extends JpaRepository<TenantMemberEntity, Long> {
    Optional<TenantMemberEntity> findOneById(Long id);
    Optional<TenantMemberEntity> findByAuthcoSub(String authcoSub);
    Optional<TenantMemberEntity> findByEmailIgnoreCaseAndAuthcoSubIsNull(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<TenantMemberEntity> findAllByOrderByCreatedAtAsc();
}
