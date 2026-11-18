package org.amupoti.pandemic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InfectionRate {

    private static final List<Integer> infectionRate = new ArrayList<>(
            Arrays.asList(2, 2, 2, 3, 3, 4, 4, 5)
    );

    /**
     * The rate stays at its last value after the last epidemic of the track
     */
    public static int getRateForEpidemic(int epidemicNumber) {
        return infectionRate.get(Math.min(epidemicNumber, infectionRate.size() - 1));
    }
}
