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

    @Test
    void epidemicRiskIsComputedPerCityFromTheBottomSet() {
        List<InfectionSet> sets = Arrays.asList(
                set("Alpha", "Beta"),
                set("Lagos 1", "Lagos 2", "Madrid", "Tokio")
        );

        List<CityEpidemicRisk> risks = InfectionDeckData.rankCitiesByEpidemicRisk(sets);

        assertThat(risks).extracting(CityEpidemicRisk::getCity).containsExactly("Lagos", "Madrid", "Tokio");
        assertThat(risks.get(0).getCardsAtBottom()).isEqualTo(2);
        assertThat(risks.get(0).getProbability()).isCloseTo(0.5, offset(0.000001));
        assertThat(risks.get(1).getProbability()).isCloseTo(0.25, offset(0.000001));
    }

    @Test
    void epidemicRiskSkipsEmptySetsAtTheBottom() {
        List<InfectionSet> sets = Arrays.asList(
                set("Alpha", "Beta"),
                set("Gamma"),
                set()
        );

        List<CityEpidemicRisk> risks = InfectionDeckData.rankCitiesByEpidemicRisk(sets);

        assertThat(risks).extracting(CityEpidemicRisk::getCity).containsExactly("Gamma");
        assertThat(risks.get(0).getProbability()).isEqualTo(1.0);
    }

    @Test
    void epidemicRiskIsEmptyWhenThereAreNoCards() {
        assertThat(InfectionDeckData.rankCitiesByEpidemicRisk(Collections.emptyList())).isEmpty();
    }

    @Test
    void repeatedCardNamesDoNotBreakTheCalculation() {
        List<InfectionSet> sets = Collections.singletonList(set("Lagos 1", "Lagos 1", "Madrid"));

        Map<String, Double> probabilities = InfectionDeckData.rankCitiesByInfectionRisk(sets, 1);

        assertThat(probabilities.get("Lagos 1")).isCloseTo(2.0 / 3.0, offset(0.000001));
    }

    @Test
    void anEmptySheetGivesAnEmptyDeckAtTheInitialInfection() {
        InfectionDeckData deck = InfectionDeckData.createFromInfectionCardData(Collections.emptyList());

        assertThat(deck.getCurrentEpidemic()).isZero();
        assertThat(deck.getPossibleCardsForNextInfectionDraws()).isEmpty();
        assertThat(deck.getInfectionCardsInCurrentEpidemic().getCardsInEpidemic()).isEmpty();
        assertThat(new EpidemicStatusData(deck).getTurnsSinceLastEpidemic()).isZero();
    }

    @Test
    void sortsTheCardsOfEachSetByRiskAndThenByName() {
        List<InfectionSet> sets = Collections.singletonList(set("Alpha", "Beta", "Gamma", "Delta"));
        Map<String, Double> probabilities = new java.util.HashMap<>();
        probabilities.put("Beta", 0.5);
        probabilities.put("Delta", 0.5);
        probabilities.put("Gamma", 0.9);

        List<InfectionSet> sorted = InfectionDeckData.sortByInfectionRisk(sets, probabilities);

        assertThat(sorted.get(0).getCardsInEpidemic())
                .extracting(InfectionCard::getCityName)
                .containsExactly("Gamma", "Beta", "Delta", "Alpha");
        assertThat(sets.get(0).getCardsInEpidemic())
                .extracting(InfectionCard::getCityName)
                .as("the original sets are left as they were")
                .containsExactly("Alpha", "Beta", "Gamma", "Delta");
    }

    @Test
    void namesWhereEachSetOfTheDeckComesFrom() {
        // Periods: initial infection, epidemic 1, epidemic 2 (current, so in the discard pile).
        // Each set is named after the phase its cards were last drawn in, like the columns of the grid
        List<InfectionCard> cards = Arrays.asList(
                drawnIn("Never", false, false, false),
                drawnIn("Initial", true, false, false),
                drawnIn("FirstEpidemic", true, true, false),
                drawnIn("Discarded", false, false, true));

        InfectionDeckData deck = InfectionDeckData.createFromInfectionCardData(cards);

        assertThat(deck.getOriginsOfNextInfectionDraws()).containsExactly("Epidèmia 1", "Infecció inicial", "Mai robades");
        assertThat(deck.getPossibleCardsForNextInfectionDraws())
                .extracting(set -> set.getCardsInEpidemic().get(0).getCityName())
                .containsExactly("FirstEpidemic", "Initial", "Never");
    }

    @Test
    void theInitialInfectionHasItsOwnName() {
        assertThat(InfectionDeckData.originName(1)).isEqualTo("Infecció inicial");
        assertThat(InfectionDeckData.originName(4)).isEqualTo("Epidèmia 3");
    }

    private static InfectionCard drawnIn(String cityName, Boolean... periods) {
        List<Boolean> appearances = new java.util.ArrayList<>();
        appearances.add(true);
        appearances.addAll(Arrays.asList(periods));
        return new InfectionCard(0, cityName, "", true, false, false, new InfectionCardAppearances(appearances), "");
    }

    private static InfectionSet set(String... cityNames) {
        return new InfectionSet(Arrays.stream(cityNames)
                .map(InfectionDeckDataTest::card)
                .collect(java.util.stream.Collectors.toList()));
    }

    private static InfectionCard card(String cityName) {
        return new InfectionCard(
                0,
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
