package org.amupoti.pandemic.model;

import lombok.Value;

@Value
public class EpidemicStatusData {

    private static final String INITIAL_EPIDEMIC = "Infección inicial";
    String currentEpidemic;
    int turnsSinceLastEpidemic;
    int infectionRate;

    public EpidemicStatusData(InfectionDeckData infectionDeckData) {
        int currentEpidemicNumber = infectionDeckData.getCurrentEpidemic();
        this.currentEpidemic = formatEpidemic(currentEpidemicNumber);
        turnsSinceLastEpidemic = computeTurns(currentEpidemicNumber, infectionDeckData.getInfectionCardsInCurrentEpidemic().getCardsInEpidemic().size());
        infectionRate = InfectionRate.getRateForEpidemic(currentEpidemicNumber);
    }

    private String formatEpidemic(int currentEpidemic) {
        if (currentEpidemic == 0) {
            return INITIAL_EPIDEMIC;
        }
        return "" + currentEpidemic;
    }

    private int computeTurns(int currentEpidemic, int infectionCardsInCurrentEpidemic) {
        return infectionCardsInCurrentEpidemic / InfectionRate.getRateForEpidemic(currentEpidemic);
    }
}
