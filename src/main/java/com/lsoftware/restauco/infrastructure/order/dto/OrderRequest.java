package com.lsoftware.restauco.infrastructure.order.dto;

import com.lsoftware.restauco.usecase.order.dto.OrderLine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/** Only dish ids and choices: names and prices are always read from the menu. */
public record OrderRequest(@NotEmpty @Size(max = 30) List<@Valid Line> lines) {

    public record Line(
            @NotNull Long dishId,
            @Min(1) @Max(20) int qty,
            @Size(max = 140) String note,
            @Size(max = 20) List<Long> removedIngredientIds,
            @Size(max = 10) Map<String, List<String>> selections
    ) {}

    public List<OrderLine> toLines() {
        return lines.stream()
                .map(l -> new OrderLine(l.dishId(), l.qty(), l.note(), l.removedIngredientIds(), l.selections()))
                .toList();
    }
}
