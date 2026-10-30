package com.lsoftware.restauco.infrastructure.menu.dto;

import com.lsoftware.restauco.entity.menu.model.ModifierGroup;
import com.lsoftware.restauco.entity.menu.model.ModifierOption;
import com.lsoftware.restauco.usecase.menu.dto.DishCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DishRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @Min(0) @Max(100_000_000) long price,
        boolean available,
        boolean active,
        int position,
        @Size(max = 40) List<@Valid IngredientRef> ingredients,
        @Size(max = 20) List<Long> tagIds,
        @Size(max = 10) List<@Valid ModifierGroupRequest> modifiers
) {
    public record IngredientRef(@NotNull Long ingredientId, boolean removable) {}

    public record ModifierGroupRequest(
            @Size(max = 40) String id,
            @NotBlank @Size(max = 60) String name,
            @Min(0) int min,
            @Min(1) int max,
            @Size(min = 1, max = 30) List<@Valid ModifierOptionRequest> options
    ) {}

    /** ingredientIds: what the option is made with, so it cannot be chosen once one runs out. */
    public record ModifierOptionRequest(
            @Size(max = 40) String id,
            @NotBlank @Size(max = 80) String name,
            @Min(0) @Max(10_000_000) long price,
            @Size(max = 10) List<Long> ingredientIds
    ) {}

    public DishCommand toCommand() {
        return new DishCommand(categoryId, name, description, price, available, active, position,
                ingredients == null ? List.of() : ingredients.stream()
                        .map(i -> new DishCommand.IngredientRef(i.ingredientId(), i.removable())).toList(),
                tagIds,
                modifiers == null ? List.of() : modifiers.stream()
                        .map(g -> new ModifierGroup(g.id(), g.name(), g.min(), g.max(), g.options() == null ? List.of()
                                : g.options().stream().map(o -> new ModifierOption(o.id(), o.name(), o.price(), o.ingredientIds(), true)).toList()))
                        .toList());
    }
}
