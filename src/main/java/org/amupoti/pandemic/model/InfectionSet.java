package org.amupoti.pandemic.model;

import lombok.Value;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Value
public class InfectionSet {

    List<InfectionCard> cardsInEpidemic;

    public static InfectionSet getEpidemicCards(List<InfectionCard> infectionCards, int epidemic) {
        return new InfectionSet(infectionCards.stream()
                .filter(card -> card.appearsInEpidemic(epidemic))
                .sorted(Comparator.comparing(InfectionCard::getCityName))
                .collect(Collectors.toList()));
    }
}
