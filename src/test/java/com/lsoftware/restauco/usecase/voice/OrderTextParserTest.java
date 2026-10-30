package com.lsoftware.restauco.usecase.voice;

import com.lsoftware.restauco.entity.menu.model.Dish;
import com.lsoftware.restauco.entity.menu.model.DishIngredient;
import com.lsoftware.restauco.entity.menu.model.ModifierGroup;
import com.lsoftware.restauco.entity.menu.model.ModifierOption;
import com.lsoftware.restauco.entity.voice.model.VoiceIssue;
import com.lsoftware.restauco.entity.voice.model.VoiceLine;
import com.lsoftware.restauco.entity.voice.model.VoiceOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Against Toche's real menu, with phrases the way waiters say them. */
class OrderTextParserTest {

    private static final List<ModifierOption> FILLINGS = List.of(
            new ModifierOption("chicharron", "Chicharrón", 0),
            new ModifierOption("chorizo", "Chorizo santarrosano", 0),
            new ModifierOption("maduritos", "Maduritos", 0),
            new ModifierOption("carne", "Carne desmechada", 0),
            new ModifierOption("cerdo", "Salteado de cerdo", 0),
            new ModifierOption("pollo", "Pollo desmechado", 0),
            new ModifierOption("codorniz", "Huevo de codorniz", 0),
            new ModifierOption("reina", "Reina pepiada (pollo desmechado y aguacate)", 0),
            new ModifierOption("tocineta", "Queso con tocineta", 0),
            new ModifierOption("costilla", "Queso con costilla", 0));

    private static final DishIngredient MASA = new DishIngredient(1L, "Masa de maíz", false, false);
    private static final DishIngredient QUESO = new DishIngredient(2L, "Queso mozzarella", true, true);
    private static final DishIngredient AGUACATE = new DishIngredient(5L, "Aguacate", false, true);
    private static final DishIngredient CARAOTAS = new DishIngredient(6L, "Caraotas", false, true);
    private static final DishIngredient TAJADAS = new DishIngredient(7L, "Tajadas de maduro", false, true);
    private static final DishIngredient POLLO = new DishIngredient(3L, "Pollo desmechado", false, false);
    private static final DishIngredient CARNE = new DishIngredient(4L, "Carne desmechada", false, false);

    private static final List<Dish> MENU = List.of(
            arepa(1L, "Arepa con 1 relleno", 1),
            arepa(2L, "Arepa con 2 rellenos", 2),
            arepa(3L, "Arepa con 4 rellenos", 4),
            dish(4L, "Arepa Sifrina", true, MASA, POLLO, QUESO, AGUACATE),
            dish(5L, "Arepa Pelúa", true, MASA, CARNE, QUESO),
            dish(6L, "Arepa de Pabellón", true, MASA, CARNE, CARAOTAS, QUESO, TAJADAS),
            dish(7L, "Arepa Dominó", true, MASA, new DishIngredient(6L, "Caraotas", false, false), QUESO),
            dish(8L, "Arepa Catira", false, MASA, POLLO, QUESO),
            dish(9L, "Jugo natural", true, new DishIngredient(8L, "Fruta natural", false, false)),
            dish(10L, "Gaseosa", true));

    private final OrderTextParser parser = new OrderTextParser(MENU);

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "dos pelúas | 5 | 2",
            "una peluda | 5 | 1",
            "tres jugos | 9 | 3",
            "2 gaseosas | 10 | 2",
            "dos gaseosas | 10 | 2",
            "una pabellón | 6 | 1",
            "un dominó | 7 | 1",
            "una reina pepiada | 1 | 1",
            "otra de codorniz | 1 | 1",
            "una arepa de chicharrón y pollo | 2 | 1",
            "arepa con dos rellenos carne y cerdo | 2 | 1",
            "una arepa de cuatro rellenos chicharrón, chorizo, carne y pollo | 3 | 1",
            "dos arepas de queso con tocineta | 1 | 2",
    })
    void understandsOneItem(String said, long dishId, int qty) {
        VoiceOrder order = parser.parse(said);
        assertThat(order.issues()).isEmpty();
        assertThat(order.lines()).singleElement().satisfies(l -> {
            assertThat(l.dishId()).isEqualTo(dishId);
            assertThat(l.qty()).isEqualTo(qty);
        });
    }

    @Test
    void aWholeTableInOneBreath() {
        VoiceOrder order = parser.parse("Para la mesa 3, dos pelúas, una sifrina sin queso ni aguacate, "
                + "una arepa de chicharrón y pollo y tres jugos de mora");

        assertThat(order.tableNumber()).isEqualTo(3);
        assertThat(order.issues()).isEmpty();
        assertThat(order.lines()).extracting(VoiceLine::dishId, VoiceLine::qty).containsExactly(
                org.assertj.core.groups.Tuple.tuple(5L, 2),
                org.assertj.core.groups.Tuple.tuple(4L, 1),
                org.assertj.core.groups.Tuple.tuple(2L, 1),
                org.assertj.core.groups.Tuple.tuple(9L, 3));
        assertThat(order.lines().get(1).removedIngredientIds()).containsExactly(2L, 5L);
        assertThat(order.lines().get(2).selections()).isEqualTo(Map.of("rellenos", List.of("chicharron", "pollo")));
        assertThat(order.lines().get(3).note()).isEqualTo("mora");
    }

    @Test
    void dishesWithoutQuantitiesAreStillApart() {
        VoiceOrder order = parser.parse("mesa cinco sifrina sin aguacate y pabellón sin tajadas");

        assertThat(order.tableNumber()).isEqualTo(5);
        assertThat(order.lines()).extracting(VoiceLine::dishId).containsExactly(4L, 6L);
        assertThat(order.lines().get(0).removedIngredientIds()).containsExactly(5L);
        assertThat(order.lines().get(1).removedIngredientIds()).containsExactly(7L);
    }

    @Test
    void whatCannotBeRemovedOrIsExtraBecomesTheNote() {
        VoiceOrder order = parser.parse("un jugo sin azúcar y una pelúa bien tostada");

        assertThat(order.lines()).extracting(VoiceLine::note).containsExactly("sin azúcar", "bien tostada");
    }

    @Test
    void missingFillingsAreAskedNotGuessed() {
        VoiceOrder order = parser.parse("una arepa de cuatro rellenos pollo y carne");

        assertThat(order.lines()).isEmpty();
        assertThat(order.issues()).singleElement().satisfies(i -> {
            assertThat(i.code()).isEqualTo("NEEDS_CHOICES");
            assertThat(i.dishId()).isEqualTo(3L);
        });
    }

    @Test
    void soldOutAndUnknownAreReported() {
        VoiceOrder order = parser.parse("una catira y dos hamburguesas");

        assertThat(order.lines()).isEmpty();
        assertThat(order.issues()).extracting(VoiceIssue::code).containsExactly("UNAVAILABLE", "NOT_UNDERSTOOD");
    }

    private static Dish arepa(Long id, String name, int fillings) {
        return new Dish(id, 1L, name, null, 10000, null, true, true, id.intValue(), List.of(MASA), List.of(),
                List.of(new ModifierGroup("rellenos", "Rellenos", fillings, fillings, FILLINGS)));
    }

    private static Dish dish(Long id, String name, boolean available, DishIngredient... ingredients) {
        return new Dish(id, 2L, name, null, 15000, null, available, true, id.intValue(), List.of(ingredients),
                List.of(), List.of());
    }
}
