package com.lsoftware.restauco.entity.menu.model;

import java.util.List;
import java.util.Set;

/**
 * One choice inside a modifier group; price is added to the dish price (0 = included).
 * ingredientIds = what it is made with: if any of them runs out, the option cannot be chosen.
 * available is worked out from the ingredient catalog on read; the stored value means nothing.
 */
public record ModifierOption(
        String id,
        String name,
        long price,
        List<Long> ingredientIds,
        boolean available
) {
    public ModifierOption {
        ingredientIds = ingredientIds != null ? List.copyOf(ingredientIds) : List.of();
    }

    public ModifierOption(String id, String name, long price) {
        this(id, name, price, List.of(), true);
    }

    public ModifierOption withStock(Set<Long> soldOut) {
        return new ModifierOption(id, name, price, ingredientIds, ingredientIds.stream().noneMatch(soldOut::contains));
    }
}
