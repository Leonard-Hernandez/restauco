package com.lsoftware.restauco.usecase.menu.dto;

import com.lsoftware.restauco.entity.menu.model.Category;
import com.lsoftware.restauco.entity.menu.model.Dish;
import com.lsoftware.restauco.entity.menu.model.Ingredient;
import com.lsoftware.restauco.entity.menu.model.Tag;

import java.util.List;

/** Everything a menu screen needs in one round trip. */
public record MenuView(
        List<Category> categories,
        List<Dish> dishes,
        List<Ingredient> ingredients,
        List<Tag> tags
) {}
