package com.lsoftware.restauco.infrastructure.config.db.mapper;

import com.lsoftware.restauco.entity.menu.model.Category;
import com.lsoftware.restauco.infrastructure.config.db.schema.CategoryEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toDomain(CategoryEntity e) {
        return new Category(e.getId(), e.getName(), e.getDescription(), e.getPosition(), e.isActive());
    }

    public CategoryEntity toEntity(Category c) {
        return CategoryEntity.builder()
                .id(c.id())
                .name(c.name())
                .description(c.description())
                .position(c.position())
                .active(c.active())
                .build();
    }
}
