package org.amupoti.pandemic.model;

import lombok.Value;

/**
 * Probability that a city is the one drawn from the bottom of the infection deck in the next epidemic
 */
@Value
public class CityEpidemicRisk {

    String city;
    String color;
    int cardsAtBottom;
    double probability;
}
