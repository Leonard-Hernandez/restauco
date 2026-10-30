package com.lsoftware.restauco.infrastructure.menu.gateway;

import com.lsoftware.restauco.entity.menu.gateway.IngredientGateway;
import com.lsoftware.restauco.entity.menu.model.Ingredient;
import com.lsoftware.restauco.infrastructure.config.db.mapper.IngredientMapper;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaIngredientRepository;
import com.lsoftware.restauco.infrastructure.config.db.schema.IngredientEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IngredientJpaGateway implements IngredientGateway {

    private final JpaIngredientRepository repo;
    private final IngredientMapper mapper;

    @Override
    public Ingredient save(Ingredient ingredient) {
        IngredientEntity entity = mapper.toEntity(ingredient);
        if (ingredient.id() != null) {
            entity.setTenantId(repo.findOneById(ingredient.id()).orElseThrow().getTenantId());
        }
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<Ingredient> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public List<Ingredient> findAll() {
        return repo.findAllByOrderByNameAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name, Long exceptId) {
        return exceptId == null ? repo.existsByNameIgnoreCase(name) : repo.existsByNameIgnoreCaseAndIdNot(name, exceptId);
    }

    // dish_ingredient rows go with it (ON DELETE CASCADE)
    @Override
    public void deleteById(Long id) {
        repo.findOneById(id).ifPresent(repo::delete);
    }
}
