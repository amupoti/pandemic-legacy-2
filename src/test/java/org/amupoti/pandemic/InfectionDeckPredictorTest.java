package org.amupoti.pandemic;

import org.junit.jupiter.api.Test;

/**
 * Created by amupoti on 14/08/2020.
 */
public class InfectionDeckPredictorTest {

    @Test
    public void givenAModelWithTwoEpidemics_whenThirdEpidemic_thenInfectionOrderIsPredictedSuccessfully() {

        //Assert the cards that have not appeared yet from 2nd epidemic, 1st, initial infection and initial deck are correct
        //One possible implementation would consist on:
        // - A list of sets, where each set represents in order, the initial infection deck, the initial infected cities,
        //cities infected during the first epidemic, the second, etc.

        //Each time a card is added into a set, is removed from the previous (or predecessor) set where it existed (the number of cards in the list is constant)
    }
}
