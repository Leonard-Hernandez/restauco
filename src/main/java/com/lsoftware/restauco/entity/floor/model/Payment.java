package com.lsoftware.restauco.entity.floor.model;

/** One part of a paid bill: how much was paid with which method. */
public record Payment(
        Long methodId,
        String name,
        long amount
) {}
