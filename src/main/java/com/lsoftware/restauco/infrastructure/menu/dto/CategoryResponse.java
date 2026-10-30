package com.lsoftware.restauco.infrastructure.menu.dto;

import com.lsoftware.restauco.entity.menu.model.Category;

public record CategoryResponse(Long id, String name, String description, int position, boolean active) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.id(), c.name(), c.description(), c.position(), c.active());
    }
}
