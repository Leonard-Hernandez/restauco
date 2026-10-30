package com.lsoftware.restauco.entity.menu.gateway;

import com.lsoftware.restauco.entity.menu.model.Dish;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DishGateway {
    Dish save(Dish dish);
    Optional<Dish> findById(Long id);
    List<Dish> findAllById(Collection<Long> ids);
    /** Ordered by category, then position. */
    List<Dish> findAll();
    void deleteById(Long id);
}
