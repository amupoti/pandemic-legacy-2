package org.amupoti.pandemic.model;

import lombok.Value;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.reverse;

@Value
public class InfectionDeckData {

    /**
     * List of set of cards which have appeared in each epidemic
     */
    List<InfectionSet> infectionSets;
    /**
     * List of set of cards which have appeared in each epidemic. When it appears in a posterior epidemic, it's removed
     * from the original set where they appeared
     */
    List<InfectionSet> remainingCardsInSets;
    int currentEpidemic;


    public InfectionDeckData(List<InfectionSet> infectionSets) {
        this.infectionSets = cloneSets(infectionSets);
        remainingCardsInSets = createInfectionDeckSets(infectionSets);

        //Deck and initial infection are the two initial groups
        currentEpidemic = remainingCardsInSets.size() - 2;
    }

    private List<InfectionSet> cloneSets(List<InfectionSet> infectionSets) {
        List<InfectionSet> newSets = new LinkedList<>();
        for (InfectionSet infectionSet : infectionSets) {
            newSets.add(new InfectionSet(new LinkedList<>(infectionSet.getCardsInEpidemic())));
        }
        return newSets;
    }

    private List<InfectionSet> createInfectionDeckSets(List<InfectionSet> infectionSets) {

        int numInfectionSets = infectionSets.size();
        for (int i = numInfectionSets - 1; i >= 0; i--) {
            List<InfectionCard> cardsInEpidemic = infectionSets.get(i).getCardsInEpidemic();
            for (int j = i - 1; j >= 0; j--) {
                List<InfectionCard> previousInfectionSet = infectionSets.get(j).getCardsInEpidemic();
                previousInfectionSet.removeAll(cardsInEpidemic);
            }
        }

        return infectionSets;
    }

    public static String prettyPrint(List<InfectionSet> infectionSets) {
        return infectionSets.stream()
                .filter(infectionSet -> !infectionSet.getCardsInEpidemic().isEmpty())
                .map(infectionSet -> "InfectionSet: " + infectionSet.getCardsInEpidemic().stream()
                        .map(InfectionCard::shortPrint)
                        .collect(Collectors.joining(",")))
                .collect(Collectors.joining("\n"));
    }

    public InfectionSet getInfectionCardsInCurrentEpidemic() {
        return infectionSets.get(infectionSets.size() - 1);
    }

    public List<InfectionSet> getPossibleCardsForNextInfection() {
        List<InfectionSet> cardsForNextInfection = remainingCardsInSets.subList(0, remainingCardsInSets.size() - 1);

        reverse(cardsForNextInfection);
        return cardsForNextInfection;
    }

}
