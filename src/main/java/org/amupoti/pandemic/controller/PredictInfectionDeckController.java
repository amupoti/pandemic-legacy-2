package org.amupoti.pandemic.controller;

import lombok.extern.slf4j.Slf4j;
import org.amupoti.pandemic.model.EpidemicStatusData;
import org.amupoti.pandemic.model.InfectionCard;
import org.amupoti.pandemic.model.InfectionDeckData;
import org.amupoti.pandemic.model.InfectionSet;
import org.amupoti.pandemic.service.InfectionModelLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class PredictInfectionDeckController {

    private static final String SPREADSHEET_ID = "1y3HicO9FcthZwH_DOC1J315JG3gcJrvUF4ssqZRyvhQ";

    @Autowired
    private InfectionModelLoader infectionModelLoader;

    @GetMapping("/predict.html")
    public String getPrediction(Model model) throws IOException, GeneralSecurityException {

        InfectionDeckData infectionDeckData = getInfectionDeckData();
        List<InfectionSet> infectionSetsInDeck = infectionDeckData.getPossibleCardsForNextInfectionDraws();
        EpidemicStatusData epidemicStatusData = new EpidemicStatusData(infectionDeckData);
        Map<String, Double> cityInfectionProbability = InfectionDeckData.rankCitiesByInfectionRisk(infectionSetsInDeck, epidemicStatusData.getInfectionRate());

        log.info("Possible cards for next infection: \n{}", InfectionDeckData.prettyPrint(infectionSetsInDeck));

        model.addAttribute("infectionDiscarded", infectionDeckData.getInfectionCardsInCurrentEpidemic());
        model.addAttribute("infectionSets", infectionSetsInDeck);
        model.addAttribute("epidemicData", epidemicStatusData);
        model.addAttribute("probability", cityInfectionProbability);

        log.info("Epidemic data: {}",model.asMap().get("epidemicData"));
        return "predict";
    }

    private InfectionDeckData getInfectionDeckData() throws IOException, GeneralSecurityException {
        List<InfectionCard> infectionCards = infectionModelLoader.loadFromGooglesheet(SPREADSHEET_ID);
        infectionCards.forEach(card -> log.info(card.toString()));
        return InfectionDeckData.createFromInfectionCardData(infectionCards);
    }
}
