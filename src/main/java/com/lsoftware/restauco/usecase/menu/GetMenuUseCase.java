package com.lsoftware.restauco.usecase.menu;

import com.lsoftware.restauco.entity.menu.gateway.CategoryGateway;
import com.lsoftware.restauco.entity.menu.gateway.DishGateway;
import com.lsoftware.restauco.entity.menu.gateway.IngredientGateway;
import com.lsoftware.restauco.entity.menu.gateway.TagGateway;
import com.lsoftware.restauco.entity.menu.model.Category;
import com.lsoftware.restauco.entity.menu.model.Dish;
import com.lsoftware.restauco.usecase.menu.dto.MenuView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GetMenuUseCase {

    private final CategoryGateway categories;
    private final DishGateway dishes;
    private final IngredientGateway ingredients;
    private final TagGateway tags;

    /** The menu guests see: active categories and dishes (sold-out ones included, flagged). */
    @Transactional(readOnly = true)
    public MenuView published() {
        List<Category> visible = categories.findAll().stream().filter(Category::active).toList();
        Set<Long> visibleIds = visible.stream().map(Category::id).collect(Collectors.toSet());
        List<Dish> listed = dishes.findAll().stream()
                .filter(d -> d.active() && visibleIds.contains(d.categoryId()))
                .toList();
        return new MenuView(visible, listed, ingredients.findAll(), tags.findAll());
    }

    /** Everything, hidden items included (menu editor). */
    @Transactional(readOnly = true)
    public MenuView full() {
        return new MenuView(categories.findAll(), dishes.findAll(), ingredients.findAll(), tags.findAll());
    }
}
