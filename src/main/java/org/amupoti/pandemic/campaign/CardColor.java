package org.amupoti.pandemic.campaign;

import org.amupoti.pandemic.service.InfectionCardFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Colours a card can have, as shown on the physical cards
 */
public enum CardColor {
    BLUE(InfectionCardFactory.LIGHT_BLUE, "Blau"),
    YELLOW(InfectionCardFactory.YELLOW, "Groc"),
    BLACK(InfectionCardFactory.GREY, "Negre"),
    RED(InfectionCardFactory.RED, "Vermell"),
    NONE("white", "Sense color");

    private final String value;
    private final String description;

    CardColor(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static List<CardColor> all() {
        return Arrays.asList(values());
    }

    /**
     * @return whether the value is one of the colours offered in the list
     */
    public static boolean isKnown(String value) {
        return Arrays.stream(values()).anyMatch(color -> color.value.equalsIgnoreCase(value));
    }

    /**
     * @return the name of a colour value, or the value itself when it is not one of the known colours
     */
    public static String describe(String value) {
        return Arrays.stream(values())
                .filter(color -> color.value.equalsIgnoreCase(value))
                .map(CardColor::getDescription)
                .findFirst()
                .orElse(value);
    }
}
