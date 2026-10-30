package com.lsoftware.restauco.entity.identity.model;

public record UserIdentity(
        String sub,
        String email,
        String name,
        boolean emailVerified
) {}
