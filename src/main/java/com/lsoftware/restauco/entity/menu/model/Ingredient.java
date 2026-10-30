package com.lsoftware.restauco.entity.menu.model;

public record Ingredient(
        Long id,
        String name,
        /** Shown as an allergy warning (dairy, gluten, nuts...). */
        boolean allergen,
        /** false = ran out today (see {@link Dish#orderable()}). */
        boolean available
) {
    public Ingredient(Long id, String name, boolean allergen) {
        this(id, name, allergen, true);
    }

    public Ingredient withAvailability(boolean value) {
        return new Ingredient(id, name, allergen, value);
    }
}
