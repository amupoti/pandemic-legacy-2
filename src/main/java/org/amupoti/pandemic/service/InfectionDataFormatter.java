package org.amupoti.pandemic.service;

import org.amupoti.pandemic.model.InfectionCard;
import org.amupoti.pandemic.model.InfectionSet;

import java.util.List;
import java.util.stream.Collectors;

public class InfectionDataFormatter {
    public static List<String> format(List<InfectionSet> infectionSets) {
        return infectionSets.stream()
                .filter(infectionSet -> !infectionSet.getCardsInEpidemic().isEmpty())
                .map(infectionSet -> infectionSet.getCardsInEpidemic().stream()
                        .map(InfectionCard::shortPrint)
                        .collect(Collectors.joining(", ")))
                .collect(Collectors.toList());
    }
}