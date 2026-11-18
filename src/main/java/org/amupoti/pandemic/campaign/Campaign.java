package org.amupoti.pandemic.campaign;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.amupoti.pandemic.model.EpidemicStatusData;
import org.amupoti.pandemic.model.InfectionCard;
import org.amupoti.pandemic.model.InfectionCardAppearances;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * State of the infection deck for the whole campaign. It replaces the Google sheet: each card keeps the infection
 * periods in which it was drawn in the current game, which is the same information as the "1" marks in the sheet
 * columns.
 * <p>
 * The deck is modelled as layers. Drawing a card puts it in the current period (the discard pile). An epidemic
 * draws a card from the bottom, puts it in the discard pile and then starts a new period, so the whole discard pile
 * becomes the new top layer of the deck.
 */
@Data
@NoArgsConstructor
public class Campaign {

    private static final Pattern NUMBERED_NAME = Pattern.compile("^(.*) (\\d+)$");

    /**
     * Number of the game being played, starting at 1
     */
    private int game = 1;
    /**
     * Number of infection periods so far in this game, counting the initial infection. A new game starts at 1
     */
    private int periods = 1;
    private List<CampaignCard> cards = new ArrayList<>();
    private List<CampaignEvent> history = new ArrayList<>();
    private List<LegacyChange> legacyLog = new ArrayList<>();

    /**
     * Builds a campaign from cards loaded from the Google sheet. The card ids are kept.
     */
    public static Campaign fromInfectionCards(List<InfectionCard> infectionCards) {
        Campaign campaign = new Campaign();
        if (infectionCards.isEmpty()) return campaign;

        //The first appearance only marks the card as part of the deck, the rest are the sheet columns
        campaign.periods = Math.max(1, infectionCards.get(0).getNumberOfEpidemics() - 1);
        for (InfectionCard card : infectionCards) {
            List<Boolean> appearances = card.getInfectionCardAppearances().getAppearsInEpidemic();
            TreeSet<Integer> drawnIn = IntStream.range(1, appearances.size())
                    .filter(appearances::get)
                    .mapToObj(column -> column - 1)
                    .collect(Collectors.toCollection(TreeSet::new));
            CardState state = CardState.fromSheet(card.isInNetwork(), card.isDestroyed(), card.isInBox6());
            campaign.cards.add(new CampaignCard(card.getId(), card.getCityName(), card.getLabel(), card.getColor(),
                    state, drawnIn));
        }
        return campaign;
    }

    /**
     * @return every card converted to the model used by the predictions, including cards out of the game
     */
    public List<InfectionCard> toInfectionCards() {
        return cards.stream().map(this::toInfectionCard).collect(Collectors.toList());
    }

    private InfectionCard toInfectionCard(CampaignCard card) {
        List<Boolean> appearances = new ArrayList<>();
        //If it's in the infection deck, it's considered first appearance
        appearances.add(Boolean.TRUE);
        for (int period = 0; period < periods; period++) {
            appearances.add(card.getDrawnInPeriods().contains(period));
        }
        CardState state = card.getState();
        return new InfectionCard(card.getId(), card.getCityName(), card.getLabel(),
                state != CardState.OUT_OF_NETWORK, state == CardState.END_PILE, state == CardState.BOX_6,
                new InfectionCardAppearances(appearances), card.getColor());
    }

    @JsonIgnore
    public int getCurrentPeriod() {
        return periods - 1;
    }

    @JsonIgnore
    public List<CampaignCard> getCardsInTheGame() {
        return cards.stream().filter(CampaignCard::isInTheGame).collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- Play

    /**
     * A card drawn from the infection deck goes to the discard pile
     */
    public void draw(int cardId) {
        CampaignCard card = cardInDeck(cardId);
        card.getDrawnInPeriods().add(getCurrentPeriod());
        history.add(new CampaignEvent(CampaignEvent.Type.DRAW, cardId));
    }

    /**
     * The card drawn from the bottom goes to the discard pile, which is then shuffled and put on top of the deck
     */
    public void epidemic(int cardId) {
        CampaignCard card = cardInDeck(cardId);
        card.getDrawnInPeriods().add(getCurrentPeriod());
        periods++;
        history.add(new CampaignEvent(CampaignEvent.Type.EPIDEMIC, cardId));
    }

    @JsonIgnore
    public Optional<CampaignEvent> getLastEvent() {
        return history.isEmpty() ? Optional.empty() : Optional.of(history.get(history.size() - 1));
    }

    /**
     * Reverts the last draw or epidemic
     */
    public void undo() {
        CampaignEvent event = getLastEvent()
                .orElseThrow(() -> new IllegalStateException("No hi ha res per desfer"));
        CampaignCard card = card(event.getCardId());
        if (event.getType() == CampaignEvent.Type.EPIDEMIC) {
            periods--;
        }
        card.getDrawnInPeriods().remove(getCurrentPeriod());
        history.remove(history.size() - 1);
    }

    public CampaignCard card(int cardId) {
        return cards.stream()
                .filter(card -> card.getId() == cardId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existeix cap carta amb id " + cardId));
    }

    private CampaignCard cardInDeck(int cardId) {
        CampaignCard card = card(cardId);
        if (!card.isInTheGame()) {
            throw new IllegalArgumentException(card.getCityName() + " no és al joc");
        }
        if (card.getDrawnInPeriods().contains(getCurrentPeriod())) {
            throw new IllegalArgumentException(card.getCityName() + " ja és a la pila de descarts");
        }
        return card;
    }

    // ---------------------------------------------------------------- Legacy changes

    /**
     * Adds new cards that have never been drawn, so they sit in the bottom layer of the deck. With more than one card
     * they are numbered after the cards of that city that already exist: "Lagos" x2 next to "Lagos 3" adds
     * "Lagos 4" and "Lagos 5".
     */
    public List<CampaignCard> addCards(String cityName, int count, String label, String color, CardState state) {
        String name = cityName == null ? "" : cityName.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("La carta necessita un nom de ciutat");
        if (count < 1 || count > 10) throw new IllegalArgumentException("Es poden afegir d'1 a 10 cartes de cop");

        List<String> names = new ArrayList<>();
        if (count == 1) {
            names.add(name);
        } else {
            int last = highestNumberOf(name);
            for (int i = 1; i <= count; i++) names.add(name + " " + (last + i));
        }

        List<CampaignCard> added = new ArrayList<>();
        for (String newName : names) {
            if (findByName(newName).isPresent()) throw new IllegalArgumentException("Ja hi ha una carta " + newName);
        }
        for (String newName : names) {
            int id = cards.stream().mapToInt(CampaignCard::getId).max().orElse(-1) + 1;
            CampaignCard card = new CampaignCard(id, newName, emptyIfNull(label), color, state, new TreeSet<>());
            cards.add(card);
            added.add(card);
            log(newName, "Carta nova (" + state.getDescription() + ")");
        }
        return added;
    }

    /**
     * Adds one card to the network, at the bottom of the deck
     */
    public CampaignCard addCard(String cityName, String label, String color) {
        return addCards(cityName, 1, label, color, CardState.IN_NETWORK).get(0);
    }

    private int highestNumberOf(String city) {
        return cards.stream()
                .map(card -> NUMBERED_NAME.matcher(card.getCityName()))
                .filter(Matcher::matches)
                .filter(matcher -> matcher.group(1).equalsIgnoreCase(city))
                .mapToInt(matcher -> Integer.parseInt(matcher.group(2)))
                .max()
                .orElse(0);
    }

    private Optional<CampaignCard> findByName(String name) {
        return cards.stream().filter(card -> card.getCityName().equalsIgnoreCase(name)).findFirst();
    }

    /**
     * Applies a legacy change to a card and writes what changed in the legacy log
     */
    public void updateCard(int cardId, String cityName, String label, String color, CardState state) {
        CampaignCard card = card(cardId);
        String oldName = card.getCityName();
        String newName = cityName == null || cityName.trim().isEmpty() ? oldName : cityName.trim();
        if (!newName.equalsIgnoreCase(oldName) && findByName(newName).isPresent()) {
            throw new IllegalArgumentException("Ja hi ha una carta " + newName);
        }

        List<String> changes = new ArrayList<>();
        if (!newName.equals(oldName)) changes.add("nom: " + oldName + " → " + newName);
        if (!emptyIfNull(label).equals(emptyIfNull(card.getLabel()))) {
            changes.add("etiqueta: " + describeLabel(card.getLabel()) + " → " + describeLabel(label));
        }
        if (!Objects.equals(color, card.getColor())) {
            changes.add("color: " + CardColor.describe(card.getColor()) + " → " + CardColor.describe(color));
        }
        if (state != card.getState()) {
            changes.add(card.getState().getDescription() + " → " + state.getDescription());
        }

        card.setCityName(newName);
        card.setLabel(emptyIfNull(label));
        card.setColor(color);
        card.setState(state);
        if (!changes.isEmpty()) log(newName, String.join(", ", changes));
    }

    /**
     * Moves a card to another state, for instance from the network to box 6
     */
    public void changeState(int cardId, CardState state) {
        CampaignCard card = card(cardId);
        updateCard(cardId, card.getCityName(), card.getLabel(), card.getColor(), state);
    }

    /**
     * Removes a card entered by mistake. Cards that have been drawn in this game cannot be removed, so the piles and
     * the undo history stay consistent: undo the draws or move the card out of the network instead.
     */
    public void deleteCard(int cardId) {
        CampaignCard card = card(cardId);
        boolean inHistory = history.stream().anyMatch(event -> event.getCardId() == cardId);
        if (!card.getDrawnInPeriods().isEmpty() || inHistory) {
            throw new IllegalArgumentException(card.getCityName()
                    + " ha sortit en aquesta partida: no es pot esborrar. Treu-la de la xarxa o desfés les accions.");
        }
        cards.remove(card);
        log(card.getCityName(), "Carta esborrada");
    }

    private void log(String card, String change) {
        legacyLog.add(new LegacyChange(game, card, change));
    }

    // ---------------------------------------------------------------- Games

    /**
     * Starts the next game: every card goes back to the infection deck and the initial infection begins. The cards
     * and their legacy changes are kept.
     */
    public void newGame() {
        game++;
        periods = 1;
        cards.forEach(card -> card.getDrawnInPeriods().clear());
        history.clear();
    }

    // ---------------------------------------------------------------- Corrections

    /**
     * Replaces the periods in which each card was drawn, like editing the "1" marks of the sheet. Used to fix mistakes
     * that undo cannot reach. The undo history is cleared because it may no longer match the piles.
     *
     * @param newPeriods number of periods, to add or remove a period by hand
     * @param drawn      for each card id, the periods in which it was drawn
     */
    public void correct(int newPeriods, java.util.Map<Integer, ? extends Collection<Integer>> drawn) {
        if (newPeriods < 1) throw new IllegalArgumentException("Hi ha d'haver almenys la infecció inicial");
        for (CampaignCard card : cards) {
            TreeSet<Integer> periodsOfCard = new TreeSet<>();
            Collection<Integer> marked = drawn.get(card.getId());
            if (marked != null) {
                marked.stream().filter(period -> period >= 0 && period < newPeriods).forEach(periodsOfCard::add);
            }
            card.setDrawnInPeriods(periodsOfCard);
        }
        periods = newPeriods;
        history.clear();
    }

    /**
     * Things that look like a mistake in the data, shown so they can be fixed. None of them blocks the game.
     * Cards that leave the network during a game are normal in a legacy campaign, so they still count here.
     */
    @JsonIgnore
    public List<String> getWarnings() {
        List<String> warnings = new ArrayList<>();
        long initialInfection = cards.stream()
                .filter(card -> card.getDrawnInPeriods().contains(0))
                .filter(card -> !card.getCityName().contains("Desalmados"))
                .count();
        if (periods > 1 && initialInfection < EpidemicStatusData.INITIAL_INFECTION_CARDS) {
            warnings.add("La infecció inicial només té " + initialInfection + " cartes i n'hauria de tenir "
                    + EpidemicStatusData.INITIAL_INFECTION_CARDS + ".");
        }
        return warnings;
    }

    private static String emptyIfNull(String text) {
        return text == null ? "" : text.trim();
    }

    private static String describeLabel(String label) {
        return label == null || label.trim().isEmpty() ? "(cap)" : "\"" + label.trim() + "\"";
    }
}
