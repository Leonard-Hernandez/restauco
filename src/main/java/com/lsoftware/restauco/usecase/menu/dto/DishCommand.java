package com.lsoftware.restauco.usecase.menu.dto;

import com.lsoftware.restauco.entity.menu.model.ModifierGroup;

import java.util.List;

public record DishCommand(
        Long categoryId,
        String name,
        String description,
        long price,
        boolean available,
        boolean active,
        int position,
        List<IngredientRef> ingredients,
        List<Long> tagIds,
        List<ModifierGroup> modifiers
) {
    public record IngredientRef(Long ingredientId, boolean removable) {}
}
