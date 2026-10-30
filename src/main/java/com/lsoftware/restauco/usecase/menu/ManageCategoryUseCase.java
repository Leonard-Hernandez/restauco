package com.lsoftware.restauco.usecase.menu;

import com.lsoftware.restauco.entity.menu.exception.CategoryNotEmptyException;
import com.lsoftware.restauco.entity.menu.exception.CategoryNotFoundException;
import com.lsoftware.restauco.entity.menu.gateway.CategoryGateway;
import com.lsoftware.restauco.entity.menu.model.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ManageCategoryUseCase {

    private final CategoryGateway gateway;

    @Transactional
    public Category create(Category category) {
        return gateway.save(new Category(null, category.name().strip(), blankToNull(category.description()),
                category.position(), category.active()));
    }

    @Transactional
    public Category update(Long id, Category category) {
        gateway.findById(id).orElseThrow(CategoryNotFoundException::new);
        return gateway.save(new Category(id, category.name().strip(), blankToNull(category.description()),
                category.position(), category.active()));
    }

    @Transactional
    public void delete(Long id) {
        gateway.findById(id).orElseThrow(CategoryNotFoundException::new);
        if (gateway.hasDishes(id)) throw new CategoryNotEmptyException();
        gateway.deleteById(id);
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
