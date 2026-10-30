package com.lsoftware.restauco.usecase.order;

import com.lsoftware.restauco.entity.menu.gateway.DishGateway;
import com.lsoftware.restauco.entity.menu.model.Dish;
import com.lsoftware.restauco.entity.menu.model.DishIngredient;
import com.lsoftware.restauco.entity.menu.model.ModifierGroup;
import com.lsoftware.restauco.entity.menu.model.ModifierOption;
import com.lsoftware.restauco.entity.order.exception.InvalidOrderException;
import com.lsoftware.restauco.entity.order.model.TicketItem;
import com.lsoftware.restauco.entity.order.model.TicketItemModifier;
import com.lsoftware.restauco.usecase.order.dto.OrderLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Turns order lines into priced ticket items, checking every choice against the menu. */
@Component
@RequiredArgsConstructor
public class OrderPricer {

    static final int MAX_LINES = 30;
    static final int MAX_QTY = 20;
    static final int MAX_NOTE = 140;

    private final DishGateway dishes;

    public List<TicketItem> price(List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new InvalidOrderException("EMPTY_ORDER", "The order has no items");
        }
        if (lines.size() > MAX_LINES) {
            throw new InvalidOrderException("ORDER_TOO_LARGE", "Too many lines in one order");
        }
        Map<Long, Dish> menu = dishes.findAllById(lines.stream().map(OrderLine::dishId).distinct().toList()).stream()
                .collect(Collectors.toMap(Dish::id, Function.identity()));
        return lines.stream().map(line -> item(line, menu.get(line.dishId()))).toList();
    }

    private TicketItem item(OrderLine line, Dish dish) {
        if (dish == null || !dish.orderable()) {
            String reason = dish != null && dish.soldOutReason() != null ? " (" + dish.soldOutReason() + " ran out)" : "";
            throw new InvalidOrderException("DISH_UNAVAILABLE",
                    (dish == null ? "A dish" : dish.name()) + " is not available" + reason);
        }
        if (line.qty() < 1 || line.qty() > MAX_QTY) {
            throw new InvalidOrderException("INVALID_QTY", "Quantity must be between 1 and " + MAX_QTY);
        }
        String note = line.note() == null || line.note().isBlank() ? null : line.note().strip();
        if (note != null && note.length() > MAX_NOTE) {
            throw new InvalidOrderException("NOTE_TOO_LONG", "Notes can have up to " + MAX_NOTE + " characters");
        }

        List<String> removed = removed(dish, line.removedIngredientIds());
        List<TicketItemModifier> modifiers = modifiers(dish, line.selections() != null ? line.selections() : Map.of());
        long unitPrice = dish.price() + modifiers.stream().mapToLong(TicketItemModifier::price).sum();
        return new TicketItem(dish.id(), dish.name(), unitPrice, line.qty(), note, removed, modifiers);
    }

    /** What the guest asked to leave out, plus what ran out and the dish can go without. */
    private static List<String> removed(Dish dish, List<Long> ids) {
        Map<Long, DishIngredient> byId = dish.ingredients().stream()
                .collect(Collectors.toMap(DishIngredient::ingredientId, Function.identity()));
        Set<String> names = new LinkedHashSet<>();
        for (Long id : ids != null ? new LinkedHashSet<>(ids) : Set.<Long>of()) {
            DishIngredient ingredient = byId.get(id);
            if (ingredient == null || !ingredient.removable()) {
                throw new InvalidOrderException("INVALID_REMOVAL",
                        dish.name() + " cannot be served without that ingredient");
            }
            names.add(ingredient.name());
        }
        dish.leftOut().forEach(i -> names.add(i.name()));
        return List.copyOf(names);
    }

    private static List<TicketItemModifier> modifiers(Dish dish, Map<String, List<String>> selections) {
        for (String groupId : selections.keySet()) {
            if (dish.modifierGroup(groupId).isEmpty()) {
                throw new InvalidOrderException("INVALID_MODIFIERS", dish.name() + " has no such option");
            }
        }
        List<TicketItemModifier> result = new ArrayList<>();
        for (ModifierGroup group : dish.modifiers()) {
            Set<String> chosen = new LinkedHashSet<>(selections.getOrDefault(group.id(), List.of()));
            if (chosen.size() < group.min() || chosen.size() > group.max()) {
                throw new InvalidOrderException("INVALID_MODIFIERS", group.min() == group.max()
                        ? "%s: choose %d for %s".formatted(group.name(), group.min(), dish.name())
                        : "%s: choose between %d and %d for %s".formatted(group.name(), group.min(), group.max(), dish.name()));
            }
            Map<String, ModifierOption> options = group.options().stream()
                    .collect(Collectors.toMap(ModifierOption::id, Function.identity()));
            for (String optionId : chosen) {
                ModifierOption option = options.get(optionId);
                if (option == null) {
                    throw new InvalidOrderException("INVALID_MODIFIERS", dish.name() + " has no such option");
                }
                if (!option.available()) {
                    throw new InvalidOrderException("OPTION_UNAVAILABLE", option.name() + " ran out");
                }
                result.add(new TicketItemModifier(group.name(), option.name(), option.price()));
            }
        }
        return result;
    }
}
