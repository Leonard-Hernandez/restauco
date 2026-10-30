package com.lsoftware.restauco.entity.report.model;

/** Units ordered of a dish and what they billed (options included). */
public record DishSales(Long dishId, String name, int units, long sales) {}
