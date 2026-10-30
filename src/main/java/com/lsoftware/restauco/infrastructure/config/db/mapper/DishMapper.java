package com.lsoftware.restauco.infrastructure.config.db.mapper;

import com.lsoftware.restauco.entity.menu.model.Dish;
import com.lsoftware.restauco.entity.menu.model.DishIngredient;
import com.lsoftware.restauco.entity.menu.model.Ingredient;
import com.lsoftware.restauco.entity.menu.model.ModifierGroup;
import com.lsoftware.restauco.infrastructure.config.db.schema.DishEntity;
import com.lsoftware.restauco.infrastructure.config.db.schema.DishIngredientEmbeddable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DishMapper {

    /** catalog fills the ingredient names, allergen flags and what ran out today. */
    public Dish toDomain(DishEntity e, Map<Long, Ingredient> catalog) {
        List<DishIngredient> ingredients = e.getIngredients().stream()
                .filter(i -> catalog.containsKey(i.getIngredientId()))
                .map(i -> {
                    Ingredient ingredient = catalog.get(i.getIngredientId());
                    return new DishIngredient(ingredient.id(), ingredient.name(), ingredient.allergen(), i.isRemovable(),
                            ingredient.available());
                })
                .sorted(Comparator.comparing(DishIngredient::ingredientId))
                .toList();
        Set<Long> soldOut = catalog.values().stream().filter(i -> !i.available()).map(Ingredient::id)
                .collect(Collectors.toSet());
        List<ModifierGroup> modifiers = e.getModifiers() != null
                ? e.getModifiers().stream().map(g -> g.withStock(soldOut)).toList() : List.of();
        return new Dish(e.getId(), e.getCategoryId(), e.getName(), e.getDescription(), e.getPrice(),
                e.getImagePath(), e.isAvailable(), e.isActive(), e.getPosition(), ingredients,
                e.getTagIds().stream().sorted().toList(), modifiers);
    }

    public DishEntity toEntity(Dish d) {
        return DishEntity.builder()
                .id(d.id())
                .categoryId(d.categoryId())
                .name(d.name())
                .description(d.description())
                .price(d.price())
                .imagePath(d.imagePath())
                .available(d.available())
                .active(d.active())
                .position(d.position())
                .ingredients(d.ingredients().stream()
                        .map(i -> new DishIngredientEmbeddable(i.ingredientId(), i.removable()))
                        .collect(Collectors.toCollection(HashSet::new)))
                .tagIds(new HashSet<>(d.tagIds()))
                .modifiers(new ArrayList<>(d.modifiers()))
                .build();
    }
}
