package com.lsoftware.restauco.entity.voice.model;

import java.util.List;
import java.util.Map;

/** A line understood from a voice order, in the same shape the waiter would send. */
public record VoiceLine(
        Long dishId,
        String dishName,
        int qty,
        List<Long> removedIngredientIds,
        /** modifier group id -> option ids */
        Map<String, List<String>> selections,
        String note
) {}
