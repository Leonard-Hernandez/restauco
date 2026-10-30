package com.lsoftware.restauco.infrastructure.config.db.mapper;

import com.lsoftware.restauco.entity.menu.model.Ingredient;
import com.lsoftware.restauco.infrastructure.config.db.schema.IngredientEntity;
import org.springframework.stereotype.Component;

@Component
public class IngredientMapper {

    public Ingredient toDomain(IngredientEntity e) {
        return new Ingredient(e.getId(), e.getName(), e.isAllergen(), e.isAvailable());
    }

    public IngredientEntity toEntity(Ingredient i) {
        return IngredientEntity.builder().id(i.id()).name(i.name()).allergen(i.allergen()).available(i.available()).build();
    }
}
