package com.lsoftware.restauco.usecase.order;

import com.lsoftware.restauco.entity.menu.model.Dish;
import com.lsoftware.restauco.entity.menu.model.DishIngredient;
import com.lsoftware.restauco.entity.menu.model.ModifierGroup;
import com.lsoftware.restauco.entity.menu.model.ModifierOption;

import java.util.List;

final class MenuFixtures {

    private MenuFixtures() {}

    static final ModifierGroup RELLENOS = new ModifierGroup("rellenos", "Rellenos", 2, 2, List.of(
            new ModifierOption("chicharron", "Chicharrón", 0),
            new ModifierOption("pollo", "Pollo desmechado", 0),
            new ModifierOption("queso-extra", "Queso extra", 2000)));

    static final Dish DOS_RELLENOS = new Dish(1L, 1L, "Arepa con 2 rellenos", null, 15000, null, true, true, 1,
            List.of(new DishIngredient(10L, "Masa de maíz", false, false)), List.of(), List.of(RELLENOS));

    static final Dish SIFRINA = new Dish(2L, 2L, "Arepa Sifrina", null, 15000, null, true, true, 1,
            List.of(new DishIngredient(10L, "Masa de maíz", false, false),
                    new DishIngredient(11L, "Aguacate", false, true),
                    new DishIngredient(12L, "Queso mozzarella", true, true)),
            List.of(), List.of());

    static final Dish AGOTADA = new Dish(3L, 2L, "Arepa Catira", null, 15000, null, false, true, 2,
            List.of(), List.of(), List.of());
}
