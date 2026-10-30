package com.lsoftware.restauco.entity.floor.model;

public record PaymentMethod(
        Long id,
        String name,
        int position,
        boolean active
) {}
