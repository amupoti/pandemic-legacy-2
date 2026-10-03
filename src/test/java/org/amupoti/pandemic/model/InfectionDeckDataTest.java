package org.amupoti.pandemic.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class InfectionDeckDataTest {

    @Test
    void calculatesProbabilityForIndividualCardsAndGroupsNumberedCityCards() {
        InfectionSet set = set(
                "Buenos Aires 1",
                "Buenos Aires 2",
                "Chicago 1",
                "El Cairo 2",
                "Jacksonville 2",
                "Los Angeles 1"
        );

        Map<String, Double> probabilities = InfectionDeckData.rankCitiesByInfectionRisk(
                Collections.singletonList(set),
                4
        );

        assertThat(probabilities.get("Buenos Aires 1")).isCloseTo(14.0 / 15.0, offset(0.000001));
        assertThat(probabilities.get("Buenos Aires 2")).isCloseTo(14.0 / 15.0, offset(0.000001));
        assertThat(probabilities.get("Chicago 1")).isCloseTo(2.0 / 3.0, offset(0.000001));
        assertThat(probabilities.get("El Cairo 2")).isCloseTo(2.0 / 3.0, offset(0.000001));
        assertThat(probabilities.get("Jacksonville 2")).isCloseTo(2.0 / 3.0, offset(0.000001));
        assertThat(probabilities.get("Los Angeles 1")).isCloseTo(2.0 / 3.0, offset(0.000001));
    }

    @Test
    void continuesDrawingFromTheNextSetWhenTheTopSetIsExhausted() {
        List<InfectionSet> sets = Arrays.asList(
                set("Alpha", "Beta"),
                set("Gamma", "Delta", "Epsilon", "Zeta")
        );

        Map<String, Double> probabilities = InfectionDeckData.rankCitiesByInfectionRisk(sets, 3);

        assertThat(probabilities.get("Alpha")).isEqualTo(1.0);
        assertThat(probabilities.get("Beta")).isEqualTo(1.0);
        assertThat(probabilities.get("Gamma")).isCloseTo(0.25, offset(0.000001));
        assertThat(probabilities.get("Zeta")).isCloseTo(0.25, offset(0.000001));
    }

    @Test
    void assignsZeroProbabilityToSetsThatCannotBeReachedByTheDraw() {
        List<InfectionSet> sets = Arrays.asList(
                set("Alpha", "Beta", "Gamma"),
                set("Delta", "Epsilon")
        );

        Map<String, Double> probabilities = InfectionDeckData.rankCitiesByInfectionRisk(sets, 2);

        assertThat(probabilities.get("Alpha")).isCloseTo(2.0 / 3.0, offset(0.000001));
        assertThat(probabilities.get("Delta")).isEqualTo(0.0);
        assertThat(probabilities.get("Epsilon")).isEqualTo(0.0);
    }

    private static InfectionSet set(String... cityNames) {
        return new InfectionSet(Arrays.stream(cityNames)
                .map(InfectionDeckDataTest::card)
                .collect(java.util.stream.Collectors.toList()));
    }

    private static InfectionCard card(String cityName) {
        return new InfectionCard(
                cityName,
                "",
                true,
                false,
                false,
                new InfectionCardAppearances(Collections.singletonList(true)),
                ""
        );
    }
}
