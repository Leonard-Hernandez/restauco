package com.lsoftware.restauco.usecase.order.dto;

import java.util.List;
import java.util.Map;

/**
 * A line as the waiter or guest sends it: only references and choices. Names and prices are
 * always taken from the menu, never from the client.
 */
public record OrderLine(
        Long dishId,
        int qty,
        String note,
        List<Long> removedIngredientIds,
        /** modifier group id -> chosen option ids */
        Map<String, List<String>> selections
) {}
