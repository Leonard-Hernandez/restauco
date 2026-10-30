package com.lsoftware.restauco.entity.menu.model;

import java.util.List;
import java.util.Set;

/**
 * Choices the guest makes for a dish ("Rellenos", "Término"). min = 0 makes it optional;
 * min = max = 1 is a single required choice.
 */
public record ModifierGroup(
        String id,
        String name,
        int min,
        int max,
        List<ModifierOption> options
) {
    public ModifierGroup {
        options = options != null ? List.copyOf(options) : List.of();
    }

    public boolean required() {
        return min > 0;
    }

    /** Enough options left to make the choice the group asks for. */
    public boolean satisfiable() {
        return options.stream().filter(ModifierOption::available).count() >= min;
    }

    public ModifierGroup withStock(Set<Long> soldOut) {
        return new ModifierGroup(id, name, min, max, options.stream().map(o -> o.withStock(soldOut)).toList());
    }
}
