package com.lsoftware.restauco.entity.menu.model;

import java.util.List;
import java.util.Optional;

public record Dish(
        Long id,
        Long categoryId,
        String name,
        String description,
        /** Whole pesos (COP has no cents in practice). */
        long price,
        /** Uploaded file name, or an absolute URL/path for demo images. Null = no photo. */
        String imagePath,
        /** false = sold out today; the dish stays on the menu but cannot be ordered. */
        boolean available,
        /** false = hidden from the menu. */
        boolean active,
        int position,
        List<DishIngredient> ingredients,
        List<Long> tagIds,
        List<ModifierGroup> modifiers
) {
    public Dish {
        ingredients = ingredients != null ? List.copyOf(ingredients) : List.of();
        tagIds = tagIds != null ? List.copyOf(tagIds) : List.of();
        modifiers = modifiers != null ? List.copyOf(modifiers) : List.of();
    }

    /**
     * Can be ordered now: listed, not marked sold out, and nothing it cannot go without ran out
     * (a required ingredient, or every option of a required choice).
     */
    public boolean orderable() {
        return active && available && soldOutReason() == null;
    }

    /** What ran out that stops the dish, or null. Marking the whole dish sold out is not a reason here. */
    public String soldOutReason() {
        for (DishIngredient i : ingredients) {
            if (!i.available() && !i.removable()) return i.name();
        }
        for (ModifierGroup g : modifiers) {
            if (!g.satisfiable()) return g.name();
        }
        return null;
    }

    /** Removable ingredients that ran out: the dish goes without them. */
    public List<DishIngredient> leftOut() {
        return ingredients.stream().filter(DishIngredient::leftOut).toList();
    }

    public Optional<ModifierGroup> modifierGroup(String groupId) {
        return modifiers.stream().filter(g -> g.id().equals(groupId)).findFirst();
    }

    public Dish withImage(String path) {
        return new Dish(id, categoryId, name, description, price, path, available, active, position,
                ingredients, tagIds, modifiers);
    }

    public Dish withAvailability(boolean value) {
        return new Dish(id, categoryId, name, description, price, imagePath, value, active, position,
                ingredients, tagIds, modifiers);
    }
}
