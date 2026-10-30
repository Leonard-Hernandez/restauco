package com.lsoftware.restauco.entity.menu.model;

public record Category(
        Long id,
        String name,
        String description,
        int position,
        boolean active
) {}
