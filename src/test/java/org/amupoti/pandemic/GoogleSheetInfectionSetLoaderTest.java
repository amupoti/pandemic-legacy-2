package org.amupoti.pandemic;

import lombok.extern.slf4j.Slf4j;
import org.amupoti.pandemic.model.InfectionCard;
import org.amupoti.pandemic.model.InfectionDeckData;
import org.amupoti.pandemic.model.InfectionSet;
import org.amupoti.pandemic.service.InfectionModelLoader;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

/**
 * Created by amupoti on 14/08/2020.
 */
@SpringBootTest
@Slf4j
public class GoogleSheetInfectionSetLoaderTest {

    private static final String SPREADSHEET_ID = "1y3HicO9FcthZwH_DOC1J315JG3gcJrvUF4ssqZRyvhQ";

    @Autowired
    private InfectionModelLoader infectionModelLoader;

    @Test
    public void givenASheetWithAllCardsAndInfectionData_whenLoadingSheet_thenInfectionDataMatches() throws IOException, GeneralSecurityException {

        List<InfectionCard> infectionCards = infectionModelLoader.loadFromGooglesheet(SPREADSHEET_ID);

        infectionCards.forEach(card -> log.info(card.toString()));
        Assertions.assertThat(infectionCards).isNotEmpty();

        InfectionDeckData infectionDeckData = InfectionSet.createFromInfectionCardData(infectionCards);
        log.info("Remaining cards in infection sets yet to be drawn: \n{}", InfectionDeckData.prettyPrint(infectionDeckData.getRemainingCardsInSets()));

        log.info("Possible cards for next infection: \n{}", InfectionDeckData.prettyPrint(infectionDeckData.getPossibleCardsForNextInfection()));
    }
}
