package com.lsoftware.restauco.usecase.menu;

import com.lsoftware.restauco.entity.menu.exception.InvalidModifiersException;
import com.lsoftware.restauco.entity.menu.model.ModifierGroup;
import com.lsoftware.restauco.entity.menu.model.ModifierOption;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModifierRulesTest {

    @Test
    void generatesStableUniqueIdsFromNames() {
        List<ModifierGroup> groups = ModifierRules.normalize(List.of(new ModifierGroup(null, "Término", 1, 1, List.of(
                new ModifierOption(null, "Jugoso", 0),
                new ModifierOption(null, "Jugoso", 0),
                new ModifierOption("bien", "Bien asado", 0)))));

        assertThat(groups.getFirst().id()).isEqualTo("termino");
        assertThat(groups.getFirst().options()).extracting(ModifierOption::id)
                .containsExactly("jugoso", "jugoso-2", "bien");
    }

    @Test
    void groupsMustBeSatisfiable() {
        assertThatThrownBy(() -> ModifierRules.normalize(List.of(new ModifierGroup(null, "Rellenos", 4, 4, List.of(
                new ModifierOption(null, "Pollo", 0), new ModifierOption(null, "Carne", 0))))))
                .isInstanceOf(InvalidModifiersException.class);
    }
}
