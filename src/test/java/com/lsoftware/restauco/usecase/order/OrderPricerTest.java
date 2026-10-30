package com.lsoftware.restauco.usecase.order;

import com.lsoftware.restauco.entity.menu.gateway.DishGateway;
import com.lsoftware.restauco.entity.order.exception.InvalidOrderException;
import com.lsoftware.restauco.entity.order.model.TicketItem;
import com.lsoftware.restauco.usecase.order.dto.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.lsoftware.restauco.usecase.order.MenuFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderPricerTest {

    private final DishGateway dishes = mock(DishGateway.class);
    private final OrderPricer pricer = new OrderPricer(dishes);

    @BeforeEach
    void setUp() {
        when(dishes.findAllById(any())).thenReturn(List.of(DOS_RELLENOS, SIFRINA, AGOTADA));
    }

    @Test
    void pricesFromTheMenuIncludingPaidOptions() {
        List<TicketItem> items = pricer.price(List.of(new OrderLine(1L, 2, " bien tostada ", null,
                Map.of("rellenos", List.of("chicharron", "queso-extra")))));

        TicketItem item = items.getFirst();
        assertThat(item.name()).isEqualTo("Arepa con 2 rellenos");
        assertThat(item.unitPrice()).isEqualTo(17000);
        assertThat(item.lineTotal()).isEqualTo(34000);
        assertThat(item.note()).isEqualTo("bien tostada");
        assertThat(item.modifiers()).extracting(m -> m.name()).containsExactly("Chicharrón", "Queso extra");
    }

    @Test
    void requiredGroupsMustBeCompleted() {
        assertThatThrownBy(() -> pricer.price(List.of(new OrderLine(1L, 1, null, null,
                Map.of("rellenos", List.of("pollo"))))))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("choose 2");
    }

    @Test
    void unknownOptionsAreRejected() {
        assertThatThrownBy(() -> pricer.price(List.of(new OrderLine(1L, 1, null, null,
                Map.of("rellenos", List.of("pollo", "langosta"))))))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void onlyRemovableIngredientsCanBeLeftOut() {
        TicketItem item = pricer.price(List.of(new OrderLine(2L, 1, null, List.of(12L), null))).getFirst();
        assertThat(item.removed()).containsExactly("Queso mozzarella");

        assertThatThrownBy(() -> pricer.price(List.of(new OrderLine(2L, 1, null, List.of(10L), null))))
                .isInstanceOf(InvalidOrderException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_REMOVAL");
    }

    @Test
    void soldOutDishesCannotBeOrdered() {
        assertThatThrownBy(() -> pricer.price(List.of(new OrderLine(3L, 1, null, null, null))))
                .isInstanceOf(InvalidOrderException.class)
                .hasFieldOrPropertyWithValue("code", "DISH_UNAVAILABLE");
    }

    @Test
    void emptyOrdersAreRejected() {
        assertThatThrownBy(() -> pricer.price(List.of()))
                .isInstanceOf(InvalidOrderException.class)
                .hasFieldOrPropertyWithValue("code", "EMPTY_ORDER");
    }
}
