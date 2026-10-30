package com.lsoftware.restauco.infrastructure.menu.gateway;

import com.lsoftware.restauco.entity.menu.gateway.TagGateway;
import com.lsoftware.restauco.entity.menu.model.Tag;
import com.lsoftware.restauco.infrastructure.config.db.mapper.TagMapper;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaTagRepository;
import com.lsoftware.restauco.infrastructure.config.db.schema.TagEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TagJpaGateway implements TagGateway {

    private final JpaTagRepository repo;
    private final TagMapper mapper;

    @Override
    public Tag save(Tag tag) {
        TagEntity entity = mapper.toEntity(tag);
        if (tag.id() != null) {
            entity.setTenantId(repo.findOneById(tag.id()).orElseThrow().getTenantId());
        }
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<Tag> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public List<Tag> findAll() {
        return repo.findAllByOrderByNameAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name, Long exceptId) {
        return exceptId == null ? repo.existsByNameIgnoreCase(name) : repo.existsByNameIgnoreCaseAndIdNot(name, exceptId);
    }

    // dish_tag rows go with it (ON DELETE CASCADE)
    @Override
    public void deleteById(Long id) {
        repo.findOneById(id).ifPresent(repo::delete);
    }
}
