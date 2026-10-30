package com.lsoftware.restauco.entity.menu.model;

/**
 * An ingredient of a dish. name, allergen and available come from the ingredient catalog on
 * read. removable = the guest may ask for the dish without it.
 */
public record DishIngredient(
        Long ingredientId,
        String name,
        boolean allergen,
        boolean removable,
        boolean available
) {
    public DishIngredient(Long ingredientId, String name, boolean allergen, boolean removable) {
        this(ingredientId, name, allergen, removable, true);
    }

    /** Ran out, but the dish can be served without it. */
    public boolean leftOut() {
        return !available && removable;
    }
}
