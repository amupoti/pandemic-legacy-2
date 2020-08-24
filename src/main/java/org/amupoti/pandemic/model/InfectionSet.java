package org.amupoti.pandemic.model;

import lombok.Value;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Value
public class InfectionSet {

    List<InfectionCard> cardsInEpidemic;

    public static InfectionDeckData createFromInfectionCardData(List<InfectionCard> infectionCards) {

        //The initial
        int numberOfSets = infectionCards.get(0).getNumberOfEpidemics();

        List<InfectionSet> infectionSetList = IntStream.range(0, numberOfSets)
                .mapToObj(currentSet -> getEpidemicCards(infectionCards, currentSet))
                .collect(Collectors.toList());
        return new InfectionDeckData(infectionSetList);
    }

    private static InfectionSet getEpidemicCards(List<InfectionCard> infectionCards, int epidemic) {
        return new InfectionSet(infectionCards.stream()
                .filter(card -> card.appearsInEpidemic(epidemic))
                .sorted(Comparator.comparing(InfectionCard::getCityName))
                .collect(Collectors.toList()));
    }
}
