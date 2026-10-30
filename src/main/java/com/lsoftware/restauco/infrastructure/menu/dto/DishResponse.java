package com.lsoftware.restauco.infrastructure.menu.dto;

import com.lsoftware.restauco.entity.menu.model.Dish;
import com.lsoftware.restauco.entity.menu.model.DishIngredient;
import com.lsoftware.restauco.entity.menu.model.ModifierGroup;

import java.util.List;

public record DishResponse(
        Long id,
        Long categoryId,
        String name,
        String description,
        long price,
        /** Relative to the API base (media/...), or absolute for demo images. Null = no photo. */
        String imageUrl,
        /** The owner's switch: false = marked sold out by hand. */
        boolean available,
        /** Can be ordered now: listed, not marked sold out, and no ingredient it needs ran out. */
        boolean orderable,
        /** What ran out that stops it (an ingredient or a choice), when orderable is false because of it. */
        String soldOutReason,
        boolean active,
        int position,
        List<IngredientLine> ingredients,
        List<Long> tagIds,
        List<ModifierGroup> modifiers
) {
    /** available = false and removable: ran out, the dish comes without it. */
    public record IngredientLine(Long id, String name, boolean allergen, boolean removable, boolean available) {
        static IngredientLine from(DishIngredient i) {
            return new IngredientLine(i.ingredientId(), i.name(), i.allergen(), i.removable(), i.available());
        }
    }

    public static DishResponse from(Dish d, String tenantSlug) {
        return new DishResponse(d.id(), d.categoryId(), d.name(), d.description(), d.price(),
                imageUrl(d.imagePath(), tenantSlug), d.available(), d.orderable(), d.soldOutReason(), d.active(), d.position(),
                d.ingredients().stream().map(IngredientLine::from).toList(), d.tagIds(), d.modifiers());
    }

    static String imageUrl(String path, String tenantSlug) {
        if (path == null) return null;
        return path.contains("/") ? path : "media/" + tenantSlug + "/" + path;
    }
}
