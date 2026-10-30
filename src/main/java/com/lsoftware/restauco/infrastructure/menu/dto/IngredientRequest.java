package com.lsoftware.restauco.infrastructure.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IngredientRequest(@NotBlank @Size(max = 80) String name, boolean allergen) {}
