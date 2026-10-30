package com.lsoftware.restauco.infrastructure.config.db.repository;

import com.lsoftware.restauco.infrastructure.config.db.schema.VoiceSampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaVoiceSampleRepository extends JpaRepository<VoiceSampleEntity, Long> {
    Optional<VoiceSampleEntity> findOneById(Long id);
}
