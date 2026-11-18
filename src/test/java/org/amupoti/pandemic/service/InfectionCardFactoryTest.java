package org.amupoti.pandemic.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class InfectionCardFactoryTest {

    private static String colorOf(String cityName) {
        return InfectionCardFactory.fromRow(1, Arrays.asList(cityName, "", "sí", "", "", "", "", "", "", "", "", "0")).getColor();
    }

    @Test
    void numberedCardsTakeTheColourOfTheirCity() {
        assertThat(colorOf("Londres 2")).isEqualTo(InfectionCardFactory.LIGHT_BLUE);
        assertThat(colorOf("Estambul 3")).isEqualTo(InfectionCardFactory.GREY);
        assertThat(colorOf("Lagos 1")).isEqualTo(InfectionCardFactory.YELLOW);
        assertThat(colorOf("Tokio")).isEqualTo(InfectionCardFactory.RED);
    }

    @Test
    void accentsAndCaseDoNotMatter() {
        assertThat(colorOf("Ciudad de México")).isEqualTo(InfectionCardFactory.YELLOW);
        assertThat(colorOf("moscu 1")).isEqualTo(InfectionCardFactory.GREY);
    }
}
