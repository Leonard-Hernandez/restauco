package com.lsoftware.restauco.entity.menu.exception;

import com.lsoftware.restauco.entity.exception.NotFoundException;

public class DishNotFoundException extends NotFoundException {
    public DishNotFoundException() {
        super("DISH_NOT_FOUND", "Dish not found");
    }
}
