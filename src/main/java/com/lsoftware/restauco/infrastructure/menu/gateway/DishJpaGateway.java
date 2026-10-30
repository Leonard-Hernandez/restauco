package com.lsoftware.restauco.infrastructure.menu.gateway;

import com.lsoftware.restauco.entity.menu.gateway.DishGateway;
import com.lsoftware.restauco.entity.menu.model.Dish;
import com.lsoftware.restauco.entity.menu.model.Ingredient;
import com.lsoftware.restauco.infrastructure.config.db.mapper.DishMapper;
import com.lsoftware.restauco.infrastructure.config.db.mapper.IngredientMapper;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaDishRepository;
import com.lsoftware.restauco.infrastructure.config.db.repository.JpaIngredientRepository;
import com.lsoftware.restauco.infrastructure.config.db.schema.DishEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DishJpaGateway implements DishGateway {

    private final JpaDishRepository repo;
    private final JpaIngredientRepository ingredients;
    private final DishMapper mapper;
    private final IngredientMapper ingredientMapper;

    @Override
    public Dish save(Dish dish) {
        DishEntity entity = mapper.toEntity(dish);
        if (dish.id() != null) {
            DishEntity existing = repo.findOneById(dish.id()).orElseThrow();
            entity.setTenantId(existing.getTenantId());
            entity.setCreatedAt(existing.getCreatedAt());
        }
        return mapper.toDomain(repo.save(entity), catalog());
    }

    @Override
    public Optional<Dish> findById(Long id) {
        return repo.findOneById(id).map(e -> mapper.toDomain(e, catalog()));
    }

    @Override
    public List<Dish> findAllById(Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return toDomain(repo.findByIdIn(ids));
    }

    @Override
    public List<Dish> findAll() {
        return toDomain(repo.findAllByOrderByCategoryIdAscPositionAscIdAsc());
    }

    @Override
    public void deleteById(Long id) {
        repo.findOneById(id).ifPresent(repo::delete);
    }

    private List<Dish> toDomain(List<DishEntity> entities) {
        Map<Long, Ingredient> catalog = catalog();
        return entities.stream().map(e -> mapper.toDomain(e, catalog)).toList();
    }

    private Map<Long, Ingredient> catalog() {
        return ingredients.findAll().stream().map(ingredientMapper::toDomain)
                .collect(Collectors.toMap(Ingredient::id, Function.identity()));
    }
}
