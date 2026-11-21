package org.amupoti.pandemic.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class EpidemicStatusDataTest {

    @Test
    void theInfectionRateFollowsTheTrack() {
        assertThat(InfectionRate.getRateForEpidemic(0)).isEqualTo(2);
        assertThat(InfectionRate.getRateForEpidemic(5)).isEqualTo(4);
        assertThat(InfectionRate.getRateForEpidemic(7)).isEqualTo(5);
    }

    @Test
    void theInfectionRateStaysAtTheLastValueAfterTheTrack() {
        assertThat(InfectionRate.getRateForEpidemic(8)).isEqualTo(5);
        assertThat(InfectionRate.getRateForEpidemic(12)).isEqualTo(5);
    }

    @Test
    void turnsAreNeverNegativeWhileTheInitialInfectionIsBeingEntered() {
        // Only 4 of the 9 initial infection cards are in the sheet so far
        InfectionDeckData deck = InfectionDeckData.createFromInfectionCardData(cards(4, Arrays.asList(true, true)));

        assertThat(new EpidemicStatusData(deck).getTurnsSinceLastEpidemic()).isZero();
    }

    @Test
    void turnsCountTheCardsDrawnAfterTheInitialInfection() {
        // 9 initial infection cards plus 4 drawn at rate 2
        InfectionDeckData deck = InfectionDeckData.createFromInfectionCardData(cards(13, Arrays.asList(true, true)));

        assertThat(new EpidemicStatusData(deck).getTurnsSinceLastEpidemic()).isEqualTo(2);
    }

    @Test
    void aWholeTurnIsLeftWhenNoCardHasBeenDrawnThisTurn() {
        // 9 initial infection cards plus 4 drawn at rate 2: two finished turns
        InfectionDeckData deck = InfectionDeckData.createFromInfectionCardData(cards(13, Arrays.asList(true, true)));

        assertThat(new EpidemicStatusData(deck).getCardsLeftThisTurn()).isEqualTo(2);
    }

    @Test
    void cardsAlreadyDrawnThisTurnAreNotLeft() {
        // 9 initial infection cards plus 5 drawn at rate 2: one card of the third turn drawn
        InfectionDeckData deck = InfectionDeckData.createFromInfectionCardData(cards(14, Arrays.asList(true, true)));

        assertThat(new EpidemicStatusData(deck).getCardsLeftThisTurn()).isEqualTo(1);
    }

    @Test
    void theRestOfTheInitialInfectionIsLeftWhileItIsBeingEntered() {
        InfectionDeckData deck = InfectionDeckData.createFromInfectionCardData(cards(4, Arrays.asList(true, true)));

        assertThat(new EpidemicStatusData(deck).getCardsLeftThisTurn()).isEqualTo(5);
    }

    @Test
    void afterAnEpidemicTheTurnUsesTheNewRate() {
        // Epidemic 4 (rate 3): 4 cards drawn since, so 2 of the second turn are left
        InfectionDeckData deck = InfectionDeckData.createFromInfectionCardData(
                cards(4, Arrays.asList(true, false, false, false, false, true)));

        assertThat(new EpidemicStatusData(deck).getCardsLeftThisTurn()).isEqualTo(2);
    }

    private static List<InfectionCard> cards(int count, List<Boolean> appearances) {
        return IntStream.range(0, count)
                .mapToObj(i -> new InfectionCard(i, "City " + i, "", true, false, false,
                        new InfectionCardAppearances(appearances), ""))
                .collect(Collectors.toList());
    }
}
