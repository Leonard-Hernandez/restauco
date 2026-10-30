package com.lsoftware.restauco.infrastructure.config.db.schema;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/** A row of dish_ingredient (owned by the dish). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DishIngredientEmbeddable {

    @Column(name = "ingredient_id", nullable = false)
    private Long ingredientId;

    @Column(nullable = false)
    private boolean removable;
}
