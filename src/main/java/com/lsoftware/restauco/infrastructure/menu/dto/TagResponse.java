package com.lsoftware.restauco.infrastructure.menu.dto;

import com.lsoftware.restauco.entity.menu.model.Tag;

public record TagResponse(Long id, String name) {
    public static TagResponse from(Tag t) {
        return new TagResponse(t.id(), t.name());
    }
}
