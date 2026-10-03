package org.amupoti.pandemic.model;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Value
@Slf4j
public class InfectionCard {

    String cityName;
    String label;
    boolean inNetwork;
    boolean destroyed;
    boolean inBox6;
    InfectionCardAppearances infectionCardAppearances;
    String color;


    public boolean appearsInEpidemic(int epidemicNumber) {
        return infectionCardAppearances.getAppearsInEpidemic().get(epidemicNumber);
    }

    /**
     * @return the number of epidemics that have happened so far, counting the initial infection phase as an epidemic too
     */
    public int getNumberOfEpidemics() {
        return infectionCardAppearances.getAppearsInEpidemic().size();
    }

    /**
     * @return the number of times the card has appeared in the sheet (initial infection and epidemics),
     * skipping the first entry, which is always true and only marks the card as part of the deck
     */
    public long getTimesAppeared() {
        List<Boolean> appearances = infectionCardAppearances.getAppearsInEpidemic();
        return appearances.subList(1, appearances.size()).stream().filter(Boolean::booleanValue).count();
    }

    public String shortPrint() {
        String cardInfo = cityName;
        if (label != null && !label.isEmpty()) cardInfo += label;
        return cardInfo;
    }

    public boolean isInTheGame() {
        return !destroyed && !inBox6 && inNetwork;
    }

    public boolean isSoulless() {
        return cityName.contains("Desalmados");
    }
}
