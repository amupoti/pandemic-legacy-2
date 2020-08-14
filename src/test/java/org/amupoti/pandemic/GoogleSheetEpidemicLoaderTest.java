package org.amupoti.pandemic;

import org.junit.jupiter.api.Test;

/**
 * Created by amupoti on 14/08/2020.
 */
public class GoogleSheetEpidemicLoaderTest {

    @Test
    public void givenASheetWithAllCardsAndInfectionData_whenLoadingSheet_thenInfectionDataMatches() {

        /*
         Load sheet into model
         Model is based of:
         - A list of InfectionCards, which contains cityName, cardNumber (1,2,3), label (to hold data about possible improvements), isOutOfTheGame
         - A list of Epidemics, each containing a list of InfectionCards
         - Current epidemic number (to indicate what to compute and current infection rate)
         */

        //Assert number of cards match
        //Assert number of cards for each city match
        //Assert number of cards in certain epidemic match
        //Assert current epidemic number
    }
}
