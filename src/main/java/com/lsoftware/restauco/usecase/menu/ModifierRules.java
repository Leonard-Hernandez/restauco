package com.lsoftware.restauco.usecase.menu;

import com.lsoftware.restauco.entity.menu.exception.IngredientNotFoundException;
import com.lsoftware.restauco.entity.menu.exception.InvalidModifiersException;
import com.lsoftware.restauco.entity.menu.model.ModifierGroup;
import com.lsoftware.restauco.entity.menu.model.ModifierOption;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Normalizes the modifier groups an admin saves: ids are stable slugs (orders reference them),
 * generated from the name when missing, and every group must be satisfiable.
 */
final class ModifierRules {

    private ModifierRules() {}

    static List<ModifierGroup> normalize(List<ModifierGroup> groups) {
        return normalize(groups, null);
    }

    /** knownIngredients: the catalog the options may name; null skips that check. */
    static List<ModifierGroup> normalize(List<ModifierGroup> groups, Set<Long> knownIngredients) {
        if (groups == null) return List.of();
        Set<String> groupIds = new HashSet<>();
        List<ModifierGroup> result = new ArrayList<>();
        for (ModifierGroup group : groups) {
            String name = required(group.name(), "Every group needs a name");
            String groupId = uniqueId(group.id(), name, groupIds);
            if (group.options().isEmpty()) {
                throw new InvalidModifiersException("\"" + name + "\" has no options");
            }
            Set<String> optionIds = new HashSet<>();
            List<ModifierOption> options = new ArrayList<>();
            for (ModifierOption option : group.options()) {
                String optionName = required(option.name(), "Every option needs a name");
                if (option.price() < 0) throw new InvalidModifiersException("Prices cannot be negative");
                List<Long> ingredientIds = option.ingredientIds().stream().distinct().toList();
                if (knownIngredients != null && !knownIngredients.containsAll(ingredientIds)) {
                    throw new IngredientNotFoundException();
                }
                options.add(new ModifierOption(uniqueId(option.id(), optionName, optionIds), optionName, option.price(),
                        ingredientIds, true));
            }
            int min = group.min();
            int max = group.max();
            if (min < 0 || max < 1 || min > max || max > options.size()) {
                throw new InvalidModifiersException("\"" + name + "\": choose between 0 and "
                        + options.size() + " options, with minimum ≤ maximum");
            }
            result.add(new ModifierGroup(groupId, name, min, max, options));
        }
        return result;
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) throw new InvalidModifiersException(message);
        return value.strip();
    }

    private static String uniqueId(String id, String name, Set<String> taken) {
        String base = slug(id != null && !id.isBlank() ? id : name);
        String candidate = base;
        for (int i = 2; !taken.add(candidate); i++) {
            candidate = base + "-" + i;
        }
        return candidate;
    }

    static String slug(String value) {
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.isEmpty()) slug = "opcion";
        return slug.length() > 40 ? slug.substring(0, 40) : slug;
    }
}
