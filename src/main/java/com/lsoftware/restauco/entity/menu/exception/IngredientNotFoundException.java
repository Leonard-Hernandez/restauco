package com.lsoftware.restauco.entity.menu.exception;

import com.lsoftware.restauco.entity.exception.NotFoundException;

public class IngredientNotFoundException extends NotFoundException {
    public IngredientNotFoundException() {
        super("INGREDIENT_NOT_FOUND", "Ingredient not found");
    }
}
