package com.lsoftware.restauco.usecase.order;

import com.lsoftware.restauco.entity.menu.gateway.DishGateway;
import com.lsoftware.restauco.entity.menu.model.Dish;
import com.lsoftware.restauco.entity.menu.model.DishIngredient;
import com.lsoftware.restauco.entity.menu.model.ModifierGroup;
import com.lsoftware.restauco.entity.menu.model.ModifierOption;
import com.lsoftware.restauco.entity.order.exception.InvalidOrderException;
import com.lsoftware.restauco.entity.order.model.TicketItem;
import com.lsoftware.restauco.usecase.order.dto.OrderLine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** What happens to dishes and orders when an ingredient runs out. */
class StockTest {

    private static final ModifierGroup RELLENOS = new ModifierGroup("rellenos", "Relleno", 1, 1, List.of(
            new ModifierOption("chicharron", "Chicharrón", 0, List.of(20L), true),
            new ModifierOption("reina", "Reina pepiada", 0, List.of(21L, 22L), true)));

    private final DishGateway dishes = mock(DishGateway.class);
    private final OrderPricer pricer = new OrderPricer(dishes);

    @Test
    void aRequiredIngredientRunningOutStopsTheDish() {
        Dish pelua = dish(List.of(new DishIngredient(4L, "Carne desmechada", false, false, false)), List.of());

        assertThat(pelua.orderable()).isFalse();
        assertThat(pelua.soldOutReason()).isEqualTo("Carne desmechada");
    }

    @Test
    void aRemovableIngredientRunningOutIsLeftOutOfTheOrder() {
        Dish sifrina = dish(List.of(new DishIngredient(11L, "Aguacate", false, true, false),
                new DishIngredient(12L, "Queso", true, true, true)), List.of());
        when(dishes.findAllById(any())).thenReturn(List.of(sifrina));

        List<TicketItem> items = pricer.price(List.of(new OrderLine(1L, 1, null, List.of(12L), null)));

        assertThat(sifrina.orderable()).isTrue();
        assertThat(items.getFirst().removed()).containsExactly("Queso", "Aguacate");
    }

    @Test
    void anOptionMadeWithWhatRanOutCannotBeChosen() {
        Dish arepa = dish(List.of(), List.of(RELLENOS.withStock(Set.of(21L))));
        when(dishes.findAllById(any())).thenReturn(List.of(arepa));

        assertThat(arepa.orderable()).isTrue();
        assertThatThrownBy(() -> pricer.price(List.of(new OrderLine(1L, 1, null, null, Map.of("rellenos", List.of("reina"))))))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("Reina pepiada");
        assertThat(pricer.price(List.of(new OrderLine(1L, 1, null, null, Map.of("rellenos", List.of("chicharron"))))))
                .hasSize(1);
    }

    @Test
    void whenNoOptionIsLeftTheDishIsSoldOut() {
        Dish arepa = dish(List.of(), List.of(RELLENOS.withStock(Set.of(20L, 22L))));

        assertThat(arepa.orderable()).isFalse();
        assertThat(arepa.soldOutReason()).isEqualTo("Relleno");
    }

    private static Dish dish(List<DishIngredient> ingredients, List<ModifierGroup> modifiers) {
        return new Dish(1L, 1L, "Arepa", null, 10000, null, true, true, 1, ingredients, List.of(), modifiers);
    }
}
