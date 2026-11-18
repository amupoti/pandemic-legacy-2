package org.amupoti.pandemic.controller;

import lombok.extern.slf4j.Slf4j;
import lombok.Value;
import org.amupoti.pandemic.campaign.Campaign;
import org.amupoti.pandemic.campaign.CampaignCard;
import org.amupoti.pandemic.campaign.CampaignService;
import org.amupoti.pandemic.campaign.CardColor;
import org.amupoti.pandemic.campaign.CardState;
import org.amupoti.pandemic.campaign.LegacyChange;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Everything that is not played turn by turn: legacy changes to the cards, starting a new game, fixing mistakes and
 * backups
 */
@Controller
@RequestMapping("/campanya")
@Slf4j
public class CampaignController {

    private static final String CAMPAIGN = "redirect:/campanya";
    private static final String GRID = "redirect:/campanya/graella";

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    // ---------------------------------------------------------------- Cards and legacy

    @GetMapping
    public String campaign(Model model, HttpServletRequest request) {
        if (!campaignService.current().isPresent()) return "redirect:/";
        Campaign campaign = campaignService.current().get();

        List<StateGroup> groups = Arrays.stream(CardState.values())
                .map(state -> new StateGroup(state, campaign.getCards().stream()
                        .filter(card -> card.getState() == state)
                        .sorted(Comparator.comparing(CampaignCard::getCityName))
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());

        List<LegacyChange> log = new ArrayList<>(campaign.getLegacyLog());
        Collections.reverse(log);

        model.addAttribute("campaign", campaign);
        model.addAttribute("groups", groups);
        model.addAttribute("states", CardState.values());
        model.addAttribute("colors", CardColor.all());
        model.addAttribute("cardsInGame", campaign.getCardsInTheGame().size());
        model.addAttribute("legacyLog", log);
        model.addAttribute("warnings", campaign.getWarnings());
        model.addAttribute("backups", campaignService.listBackups().stream()
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList()));
        model.addAttribute("campaignFile", campaignService.getCampaignFile());
        model.addAttribute("phoneUrls", LocalNetwork.urls(request.getLocalPort()));
        return "campaign";
    }

    @PostMapping("/cards/{cardId}")
    public String updateCard(@PathVariable int cardId,
                             @RequestParam(defaultValue = "") String cityName,
                             @RequestParam(defaultValue = "") String label,
                             @RequestParam String color,
                             @RequestParam CardState state,
                             RedirectAttributes redirect) {
        return apply(() -> campaignService.updateCard(cardId, cityName, label, color, state),
                "Carta desada.", redirect, CAMPAIGN + "#card-" + cardId);
    }

    @PostMapping("/cards/{cardId}/delete")
    public String deleteCard(@PathVariable int cardId, RedirectAttributes redirect) {
        return apply(() -> campaignService.deleteCard(cardId), "Carta esborrada.", redirect, CAMPAIGN);
    }

    @PostMapping("/cards")
    public String addCards(@RequestParam String cityName,
                           @RequestParam(defaultValue = "1") int count,
                           @RequestParam(defaultValue = "") String label,
                           @RequestParam String color,
                           @RequestParam(defaultValue = "IN_NETWORK") CardState state,
                           RedirectAttributes redirect) {
        try {
            List<CampaignCard> added = campaignService.addCards(cityName, count, label, color, state);
            String names = added.stream().map(CampaignCard::getCityName).collect(Collectors.joining(", "));
            String where = state == CardState.IN_NETWORK ? "al fons de la pila" : "(" + state.getDescription() + ")";
            redirect.addFlashAttribute("message", "Cartes afegides " + where + ": " + names + ".");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return CAMPAIGN;
    }

    // ---------------------------------------------------------------- Games

    @PostMapping("/new-game")
    public String newGame(@RequestParam(defaultValue = "false") boolean confirm, RedirectAttributes redirect) {
        if (!confirm) {
            redirect.addFlashAttribute("error", "Marca la casella de confirmació per començar una partida nova.");
            return CAMPAIGN + "#nova-partida";
        }
        try {
            campaignService.newGame();
            int game = campaignService.current().map(Campaign::getGame).orElse(0);
            redirect.addFlashAttribute("message", "Partida " + game + " començada. Toca les 9 cartes de la infecció inicial.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/";
    }

    // ---------------------------------------------------------------- Corrections

    /**
     * The same table as the Google sheet: one row per card and one column per infection period of this game
     */
    @GetMapping("/graella")
    public String grid(Model model) {
        if (!campaignService.current().isPresent()) return "redirect:/";
        Campaign campaign = campaignService.current().get();
        List<CampaignCard> cards = campaign.getCards().stream()
                .sorted(Comparator.comparing(CampaignCard::isInTheGame).reversed()
                        .thenComparing(CampaignCard::getCityName))
                .collect(Collectors.toList());
        List<String> periodNames = IntStream.range(0, campaign.getPeriods())
                .mapToObj(period -> period == 0 ? "Inicial" : "Ep" + period)
                .collect(Collectors.toList());

        model.addAttribute("campaign", campaign);
        model.addAttribute("cards", cards);
        model.addAttribute("periodNames", periodNames);
        model.addAttribute("warnings", campaign.getWarnings());
        return "grid";
    }

    /**
     * Saves the whole table. Each ticked box arrives as a parameter named "c{cardId}p{period}"
     */
    @PostMapping("/graella")
    public String saveGrid(@RequestParam int periods,
                           @RequestParam Map<String, String> params,
                           RedirectAttributes redirect) {
        Map<Integer, Set<Integer>> drawn = new HashMap<>();
        params.keySet().stream()
                .filter(name -> name.matches("c\\d+p\\d+"))
                .forEach(name -> {
                    String[] parts = name.substring(1).split("p");
                    drawn.computeIfAbsent(Integer.parseInt(parts[0]), id -> new TreeSet<>()).add(Integer.parseInt(parts[1]));
                });
        return apply(() -> campaignService.correct(periods, drawn), "Correccions desades.", redirect, GRID);
    }

    // ---------------------------------------------------------------- Backups

    @GetMapping("/export")
    public ResponseEntity<byte[]> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"campanya-" + LocalDate.now() + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(campaignService.export());
    }

    @PostMapping("/restore")
    public String restore(@RequestParam("file") MultipartFile file,
                          @RequestParam(defaultValue = "false") boolean confirm,
                          RedirectAttributes redirect) {
        if (!confirm) {
            redirect.addFlashAttribute("error", "Marca la casella de confirmació per substituir la campanya.");
            return CAMPAIGN + "#copies";
        }
        try (InputStream content = file.getInputStream()) {
            campaignService.restore(content);
            redirect.addFlashAttribute("message", "Campanya restaurada des de " + file.getOriginalFilename() + ".");
        } catch (IOException | IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return CAMPAIGN;
    }

    /**
     * Overwrites the campaign with the Google sheet. Needs an explicit confirmation because local changes are lost
     */
    @PostMapping("/import")
    public String reimport(@RequestParam(defaultValue = "false") boolean confirm, RedirectAttributes redirect) {
        if (!confirm) {
            redirect.addFlashAttribute("error", "Marca la casella de confirmació per substituir la campanya.");
            return CAMPAIGN;
        }
        try {
            Campaign campaign = campaignService.importFromSheet();
            redirect.addFlashAttribute("message", "Campanya reimportada: " + campaign.getCards().size() + " cartes.");
        } catch (Exception e) {
            log.error("Import from Google Sheets failed", e);
            redirect.addFlashAttribute("error", "No s'ha pogut llegir el Google Sheets: " + e.getMessage());
        }
        return CAMPAIGN;
    }

    private String apply(Runnable action, String message, RedirectAttributes redirect, String target) {
        try {
            action.run();
            redirect.addFlashAttribute("message", message);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return target;
    }

    /**
     * The cards in one state, for the list of the campaign page
     */
    @Value
    public static class StateGroup {
        CardState state;
        List<CampaignCard> cards;
    }
}
