package org.amupoti.pandemic.model;

import lombok.Value;

import static java.util.function.Predicate.not;

@Value
public class EpidemicStatusData {

    private static final String INITIAL_EPIDEMIC = "Infecció inicial";
    public static final int INITIAL_INFECTION_CARDS = 9;
    String currentEpidemic;
    int turnsSinceLastEpidemic;
    int infectionRate;
    /**
     * Cards still to be drawn in the current infection step: the rest of the turn, or of the initial infection
     * while it is being entered. A finished turn means a whole new turn is next.
     */
    int cardsLeftThisTurn;

    public EpidemicStatusData(InfectionDeckData infectionDeckData) {
        int currentEpidemicNumber = infectionDeckData.getCurrentEpidemic();
        this.currentEpidemic = formatEpidemic(currentEpidemicNumber);
        infectionRate = InfectionRate.getRateForEpidemic(currentEpidemicNumber);
        int cardsDrawnInTurns = cardsDrawnInTurns(currentEpidemicNumber, infectionDeckData.getInfectionCardsInCurrentEpidemic());
        turnsSinceLastEpidemic = Math.max(0, cardsDrawnInTurns) / infectionRate;
        cardsLeftThisTurn = cardsDrawnInTurns < 0
                ? -cardsDrawnInTurns
                : infectionRate - cardsDrawnInTurns % infectionRate;
    }

    private String formatEpidemic(int currentEpidemic) {
        if (currentEpidemic == 0) {
            return INITIAL_EPIDEMIC;
        }
        return "" + currentEpidemic;
    }

    /**
     * @return cards drawn in turns since the last epidemic, or minus the cards still missing from the initial infection
     */
    private int cardsDrawnInTurns(int currentEpidemic, InfectionSet infectionCardsInCurrentEpidemic) {
        int cards = (int) infectionCardsInCurrentEpidemic.getCardsInEpidemic().stream()
                .filter(not(InfectionCard::isSoulless))
                .count();
        if (currentEpidemic == 0) cards -= INITIAL_INFECTION_CARDS;
        return cards;
    }
}
