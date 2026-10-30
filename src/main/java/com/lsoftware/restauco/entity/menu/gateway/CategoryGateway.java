package com.lsoftware.restauco.entity.menu.gateway;

import com.lsoftware.restauco.entity.menu.model.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryGateway {
    Category save(Category category);
    Optional<Category> findById(Long id);
    List<Category> findAll();
    boolean hasDishes(Long id);
    void deleteById(Long id);
}
