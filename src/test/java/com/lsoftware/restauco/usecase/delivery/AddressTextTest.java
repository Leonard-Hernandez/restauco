package com.lsoftware.restauco.usecase.delivery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AddressTextTest {

    @Test
    void spellsOutStreetTypesForTheMap() {
        assertThat(AddressText.forSearch("Cl. 5B # 36-20")).isEqualTo("calle 5b 36 20");
        assertThat(AddressText.forSearch("Kra 39 No 5 - 30")).isEqualTo("carrera 39 5 30");
        assertThat(AddressText.forSearch("Cra. 4 # 5 b-12")).isEqualTo("carrera 4 5b 12");
    }

    @Test
    void theStreetUnderThePinMatchesTheWrittenAddress() {
        assertThat(AddressText.sameStreet("Cl. 5B # 36-20", "Calle 5B")).isTrue();
        assertThat(AddressText.sameStreet("calle 5 b #36-20 apto 301", "Calle 5B")).isTrue();
        assertThat(AddressText.sameStreet("Avenida Roosevelt # 30-12", "Avenida Roosevelt")).isTrue();
    }

    @Test
    void anotherStreetDoesNotMatch() {
        // The pin is on the cross street, or somewhere else
        assertThat(AddressText.sameStreet("Carrera 36 # 5B-20", "Calle 5B")).isFalse();
        assertThat(AddressText.sameStreet("Calle 9 # 44-35", "Calle 5B")).isFalse();
        assertThat(AddressText.sameStreet("Cl 5B # 36-20", null)).isFalse();
    }
}
