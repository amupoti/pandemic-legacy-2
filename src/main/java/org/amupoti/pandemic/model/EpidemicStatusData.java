package org.amupoti.pandemic.model;

import lombok.Value;

import static java.util.function.Predicate.not;

@Value
public class EpidemicStatusData {

    private static final String INITIAL_EPIDEMIC = "Infección inicial";
    public static final int INITIAL_INFECTION_CARDS = 9;
    String currentEpidemic;
    int turnsSinceLastEpidemic;
    int infectionRate;

    public EpidemicStatusData(InfectionDeckData infectionDeckData) {
        int currentEpidemicNumber = infectionDeckData.getCurrentEpidemic();
        this.currentEpidemic = formatEpidemic(currentEpidemicNumber);
        turnsSinceLastEpidemic = computeTurns(currentEpidemicNumber, infectionDeckData.getInfectionCardsInCurrentEpidemic());
        infectionRate = InfectionRate.getRateForEpidemic(currentEpidemicNumber);
    }

    private String formatEpidemic(int currentEpidemic) {
        if (currentEpidemic == 0) {
            return INITIAL_EPIDEMIC;
        }
        return "" + currentEpidemic;
    }

    private int computeTurns(int currentEpidemic, InfectionSet infectionCardsInCurrentEpidemic) {
        int cards = (int) infectionCardsInCurrentEpidemic.getCardsInEpidemic().stream()
                .filter(not(InfectionCard::isSoulless))
                .count();
        if (currentEpidemic == 0) cards -= INITIAL_INFECTION_CARDS;
        return cards / InfectionRate.getRateForEpidemic(currentEpidemic);
    }
}
