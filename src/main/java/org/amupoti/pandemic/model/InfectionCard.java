package org.amupoti.pandemic.model;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;

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

    public String shortPrint() {
        String cardInfo = cityName;
        if (label != null && !label.isEmpty()) cardInfo += "(" + label + ")";
        return cardInfo;
    }

    public boolean isInTheGame() {
        return !destroyed && !inBox6 && inNetwork;
    }
}
