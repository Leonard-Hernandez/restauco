package com.lsoftware.restauco.infrastructure.menu.gateway;

import com.lsoftware.restauco.entity.menu.gateway.CategoryGateway;
import com.lsoftware.restauco.entity.menu.model.Category;
import com.lsoftware.restauco.infrastructure.config.db.mapper.CategoryMapper;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaCategoryRepository;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaDishRepository;
import com.lsoftware.restauco.infrastructure.config.db.schema.CategoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoryJpaGateway implements CategoryGateway {

    private final JpaCategoryRepository repo;
    private final JpaDishRepository dishes;
    private final CategoryMapper mapper;

    @Override
    public Category save(Category category) {
        CategoryEntity entity = mapper.toEntity(category);
        if (category.id() != null) {
            CategoryEntity existing = repo.findOneById(category.id()).orElseThrow();
            entity.setTenantId(existing.getTenantId());
            entity.setCreatedAt(existing.getCreatedAt());
        }
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<Category> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return repo.findAllByOrderByPositionAscIdAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean hasDishes(Long id) {
        return dishes.existsByCategoryId(id);
    }

    @Override
    public void deleteById(Long id) {
        repo.findOneById(id).ifPresent(repo::delete);
    }
}
