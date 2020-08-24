package org.amupoti.pandemic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InfectionRate {

    private static final List<Integer> infectionRate = new ArrayList<>(
            Arrays.asList(2, 2, 2, 3, 3, 4, 4, 5)
    );

    public static int getRateForEpidemic(int epidemicNumber) {
        return infectionRate.get(epidemicNumber);
    }
}
