package com.lsoftware.restauco.entity.menu.gateway;

import com.lsoftware.restauco.entity.menu.model.Ingredient;

import java.util.List;
import java.util.Optional;

public interface IngredientGateway {
    Ingredient save(Ingredient ingredient);
    Optional<Ingredient> findById(Long id);
    List<Ingredient> findAll();
    boolean existsByName(String name, Long exceptId);
    /** Also removes it from every dish. */
    void deleteById(Long id);
}
