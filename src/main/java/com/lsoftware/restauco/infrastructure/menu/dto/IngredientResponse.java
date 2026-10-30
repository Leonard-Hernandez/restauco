package com.lsoftware.restauco.infrastructure.menu.dto;

import com.lsoftware.restauco.entity.menu.model.Ingredient;

/** available = false: ran out today. */
public record IngredientResponse(Long id, String name, boolean allergen, boolean available) {
    public static IngredientResponse from(Ingredient i) {
        return new IngredientResponse(i.id(), i.name(), i.allergen(), i.available());
    }
}
