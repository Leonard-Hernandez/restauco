package com.lsoftware.restauco.infrastructure.voice.gateway;

import com.lsoftware.restauco.entity.voice.gateway.VoiceSampleGateway;
import com.lsoftware.restauco.entity.voice.model.VoiceSample;
import com.lsoftware.restauco.infrastructure.config.db.mapper.VoiceSampleMapper;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaVoiceSampleRepository;
import com.lsoftware.restauco.infrastructure.config.db.schema.VoiceSampleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VoiceSampleJpaGateway implements VoiceSampleGateway {

    private final JpaVoiceSampleRepository repo;
    private final VoiceSampleMapper mapper;

    @Override
    public VoiceSample save(VoiceSample sample) {
        VoiceSampleEntity entity = mapper.toEntity(sample);
        if (sample.id() != null) {
            entity.setTenantId(repo.findOneById(sample.id()).orElseThrow().getTenantId());
        }
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<VoiceSample> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }
}
