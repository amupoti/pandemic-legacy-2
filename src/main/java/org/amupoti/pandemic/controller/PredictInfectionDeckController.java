package org.amupoti.pandemic.controller;

import lombok.extern.slf4j.Slf4j;
import org.amupoti.pandemic.campaign.Campaign;
import org.amupoti.pandemic.campaign.CampaignEvent;
import org.amupoti.pandemic.campaign.CampaignService;
import org.amupoti.pandemic.model.EpidemicStatusData;
import org.amupoti.pandemic.model.InfectionCard;
import org.amupoti.pandemic.model.InfectionDeckData;
import org.amupoti.pandemic.model.InfectionSet;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class PredictInfectionDeckController {

    private static final int INITIAL_INFECTION_CARDS = 9;

    static final String EPIDEMIC_MODE = "epidemia";

    private final CampaignService campaignService;

    public PredictInfectionDeckController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @GetMapping("/")
    public String getPrediction(@RequestParam(required = false) String mode, Model model) {
        Optional<Campaign> current = campaignService.current();
        if (!current.isPresent()) {
            model.addAttribute("spreadsheetId", campaignService.getSpreadsheetId());
            model.addAttribute("campaignFile", campaignService.getCampaignFile());
            model.addAttribute("sheetConfigured", campaignService.isSheetConfigured());
            return "setup";
        }
        Campaign campaign = current.get();

        List<InfectionCard> cardsInGame = campaign.toInfectionCards().stream()
                .filter(InfectionCard::isInTheGame)
                .collect(Collectors.toList());
        InfectionDeckData infectionDeckData = InfectionDeckData.createFromInfectionCardData(cardsInGame);
        List<InfectionSet> infectionSetsInDeck = infectionDeckData.getPossibleCardsForNextInfectionDraws();
        EpidemicStatusData epidemicStatusData = new EpidemicStatusData(infectionDeckData);
        Map<String, Double> cityInfectionProbability = InfectionDeckData.rankCitiesByInfectionRisk(infectionSetsInDeck, epidemicStatusData.getInfectionRate());

        model.addAttribute("infectionDiscarded", infectionDeckData.getInfectionCardsInCurrentEpidemic());
        model.addAttribute("infectionSets", infectionSetsInDeck);
        model.addAttribute("epidemicData", epidemicStatusData);
        model.addAttribute("probability", cityInfectionProbability);
        model.addAttribute("epidemicRisk", InfectionDeckData.rankCitiesByEpidemicRisk(infectionSetsInDeck));
        model.addAttribute("epidemicMode", EPIDEMIC_MODE.equals(mode));
        model.addAttribute("game", campaign.getGame());
        model.addAttribute("warnings", campaign.getWarnings());
        // Before the first epidemic the turns' draws join the initial infection, so only count until it is complete
        int drawnInCurrentPeriod = infectionDeckData.getInfectionCardsInCurrentEpidemic().getCardsInEpidemic().size();
        model.addAttribute("initialInfection", campaign.getCurrentPeriod() == 0 && drawnInCurrentPeriod < INITIAL_INFECTION_CARDS
                ? drawnInCurrentPeriod : null);
        model.addAttribute("lastAction", campaign.getLastEvent().map(event -> describe(campaign, event)).orElse(null));
        return "predict";
    }

    @PostMapping("/draw/{cardId}")
    public String draw(@PathVariable int cardId, RedirectAttributes redirect) {
        return apply(() -> campaignService.draw(cardId), redirect, "redirect:/");
    }

    @PostMapping("/epidemic/{cardId}")
    public String epidemic(@PathVariable int cardId, RedirectAttributes redirect) {
        //On error, stay in epidemic mode so the right card can be picked
        return apply(() -> campaignService.epidemic(cardId), redirect, "redirect:/?mode=" + EPIDEMIC_MODE, "redirect:/");
    }

    @PostMapping("/undo")
    public String undo(RedirectAttributes redirect) {
        return apply(campaignService::undo, redirect, "redirect:/");
    }

    @PostMapping("/new-campaign")
    public String newCampaign(RedirectAttributes redirect) {
        try {
            campaignService.startEmpty();
            redirect.addFlashAttribute("message", "Campanya creada. Afegeix les cartes de la pila d'infecció.");
            return "redirect:/campanya#afegir";
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/import")
    public String importFromSheet(RedirectAttributes redirect) {
        try {
            Campaign campaign = campaignService.importFromSheet();
            redirect.addFlashAttribute("message", "Campanya importada: " + campaign.getCards().size() + " cartes.");
        } catch (Exception e) {
            log.error("Import from Google Sheets failed", e);
            redirect.addFlashAttribute("error", "No s'ha pogut llegir el Google Sheets: " + e.getMessage());
        }
        return "redirect:/";
    }

    private String apply(Runnable action, RedirectAttributes redirect, String onSuccess) {
        return apply(action, redirect, onSuccess, onSuccess);
    }

    private String apply(Runnable action, RedirectAttributes redirect, String onError, String onSuccess) {
        try {
            action.run();
            return onSuccess;
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return onError;
        }
    }

    private static String describe(Campaign campaign, CampaignEvent event) {
        String city = campaign.card(event.getCardId()).getCityName();
        return event.getType() == CampaignEvent.Type.EPIDEMIC ? "Epidèmia a " + city : "Robada: " + city;
    }
}
