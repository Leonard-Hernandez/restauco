package com.lsoftware.restauco.infrastructure.menu.dto;

import com.lsoftware.restauco.usecase.menu.dto.MenuView;

import java.util.List;

public record MenuResponse(
        List<CategoryResponse> categories,
        List<DishResponse> dishes,
        List<IngredientResponse> ingredients,
        List<TagResponse> tags
) {
    public static MenuResponse from(MenuView v, String tenantSlug) {
        return new MenuResponse(
                v.categories().stream().map(CategoryResponse::from).toList(),
                v.dishes().stream().map(d -> DishResponse.from(d, tenantSlug)).toList(),
                v.ingredients().stream().map(IngredientResponse::from).toList(),
                v.tags().stream().map(TagResponse::from).toList());
    }
}
