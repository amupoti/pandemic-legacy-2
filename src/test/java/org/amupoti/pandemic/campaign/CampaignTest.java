package org.amupoti.pandemic.campaign;

import org.amupoti.pandemic.model.CityEpidemicRisk;
import org.amupoti.pandemic.model.InfectionCard;
import org.amupoti.pandemic.model.InfectionCardAppearances;
import org.amupoti.pandemic.model.InfectionDeckData;
import org.amupoti.pandemic.model.InfectionSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampaignTest {

    private static final int ALPHA = 0;
    private static final int BETA = 1;
    private static final int GAMMA = 2;
    private static final int DELTA = 3;
    private static final int EPSILON = 4;

    private Campaign campaign;

    /**
     * Initial infection drew Alpha and Beta. Gamma, Delta and Epsilon have never been drawn.
     */
    @BeforeEach
    void setUp() {
        campaign = new Campaign();
        Arrays.asList("Alpha", "Beta", "Gamma", "Delta", "Epsilon")
                .forEach(name -> campaign.addCard(name, "", "white"));
        campaign.draw(ALPHA);
        campaign.draw(BETA);
        campaign.getHistory().clear();
    }

    @Test
    void importsTheSheetColumnsAsPeriods() {
        List<InfectionCard> sheetCards = Arrays.asList(
                sheetCard(7, "Lagos", true, false, true),
                sheetCard(8, "Lima", false, true, false));

        Campaign imported = Campaign.fromInfectionCards(sheetCards);

        assertThat(imported.getPeriods()).isEqualTo(3);
        assertThat(imported.card(7).getDrawnInPeriods()).containsExactly(0, 2);
        assertThat(imported.card(8).getDrawnInPeriods()).containsExactly(1);
        assertThat(imported.toInfectionCards()).isEqualTo(sheetCards);
    }

    @Test
    void drawingACardPutsItInTheDiscardPile() {
        campaign.draw(GAMMA);

        assertThat(discardPile()).containsExactly("Alpha", "Beta", "Gamma");
        assertThat(deckLayers()).containsExactly(Arrays.asList("Delta", "Epsilon"));
    }

    @Test
    void cannotDrawACardThatIsAlreadyInTheDiscardPile() {
        assertThatThrownBy(() -> campaign.draw(ALPHA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Alpha");
    }

    @Test
    void cannotDrawACardThatIsOutOfTheGame() {
        campaign.changeState(GAMMA, CardState.END_PILE);

        assertThatThrownBy(() -> campaign.draw(GAMMA)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotDrawAnUnknownCard() {
        assertThatThrownBy(() -> campaign.draw(99)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anEpidemicPutsTheBottomCardAndTheDiscardPileOnTopOfTheDeck() {
        campaign.epidemic(EPSILON);

        assertThat(epidemicNumber()).isEqualTo(1);
        assertThat(discardPile()).isEmpty();
        assertThat(deckLayers()).containsExactly(
                Arrays.asList("Alpha", "Beta", "Epsilon"),
                Arrays.asList("Delta", "Gamma"));
    }

    @Test
    void afterAnEpidemicTheNextBottomLayerIsTheOldestOne() {
        campaign.epidemic(EPSILON);
        campaign.draw(ALPHA);

        List<CityEpidemicRisk> risks = InfectionDeckData.rankCitiesByEpidemicRisk(deckData().getPossibleCardsForNextInfectionDraws());

        assertThat(risks).extracting(CityEpidemicRisk::getCity).containsExactly("Delta", "Gamma");
    }

    @Test
    void undoRevertsADraw() {
        campaign.draw(GAMMA);

        campaign.undo();

        assertThat(discardPile()).containsExactly("Alpha", "Beta");
        assertThat(campaign.getHistory()).isEmpty();
    }

    @Test
    void undoRevertsAnEpidemic() {
        campaign.epidemic(EPSILON);

        campaign.undo();

        assertThat(epidemicNumber()).isEqualTo(0);
        assertThat(discardPile()).containsExactly("Alpha", "Beta");
        assertThat(deckLayers()).containsExactly(Arrays.asList("Delta", "Epsilon", "Gamma"));
    }

    @Test
    void undoCanGoBackSeveralSteps() {
        campaign.draw(GAMMA);
        campaign.epidemic(EPSILON);
        campaign.draw(DELTA);

        campaign.undo();
        campaign.undo();
        campaign.undo();

        assertThat(discardPile()).containsExactly("Alpha", "Beta");
        assertThat(epidemicNumber()).isEqualTo(0);
    }

    @Test
    void cannotUndoWithoutHistory() {
        assertThatThrownBy(() -> campaign.undo()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void newCardsGetTheNextIdAndStartAtTheBottom() {
        CampaignCard card = campaign.addCard(" Kinsasa ", "Refugi", "#E9B824");

        assertThat(card.getId()).isEqualTo(5);
        assertThat(card.getCityName()).isEqualTo("Kinsasa");
        assertThat(deckLayers()).containsExactly(Arrays.asList("Delta", "Epsilon", "Gamma", "Kinsasa"));
    }

    @Test
    void cardsNeedAName() {
        assertThatThrownBy(() -> campaign.addCard(" ", "", "white")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatingACardChangesItsLegacyState() {
        campaign.updateCard(GAMMA, "Gamma", "Bloqueig", "#E74C3C", CardState.BOX_6);

        CampaignCard gamma = campaign.card(GAMMA);
        assertThat(gamma.getLabel()).isEqualTo("Bloqueig");
        assertThat(gamma.getColor()).isEqualTo("#E74C3C");
        assertThat(gamma.getState()).isEqualTo(CardState.BOX_6);
        assertThat(gamma.isInTheGame()).isFalse();
    }

    @Test
    void aCardCanMoveBetweenAnyStatesInBothDirections() {
        for (CardState state : CardState.values()) {
            campaign.changeState(GAMMA, state);
            assertThat(campaign.card(GAMMA).getState()).isEqualTo(state);
            assertThat(campaign.card(GAMMA).isInTheGame()).isEqualTo(state == CardState.IN_NETWORK);
        }
        campaign.changeState(GAMMA, CardState.IN_NETWORK);

        assertThat(deckLayers()).containsExactly(Arrays.asList("Delta", "Epsilon", "Gamma"));
    }

    @Test
    void aCardOutOfTheNetworkLeavesThePilesAndComesBackToTheSameLayer() {
        campaign.changeState(ALPHA, CardState.OUT_OF_NETWORK);
        assertThat(discardPile()).containsExactly("Beta");

        campaign.changeState(ALPHA, CardState.IN_NETWORK);
        assertThat(discardPile()).containsExactly("Alpha", "Beta");
    }

    @Test
    void legacyChangesAreLogged() {
        campaign.updateCard(GAMMA, "Gamma 1", "Refugi", "#E74C3C", CardState.BOX_6);

        LegacyChange change = lastChange();
        assertThat(change.getGame()).isEqualTo(1);
        assertThat(change.getCard()).isEqualTo("Gamma 1");
        assertThat(change.getChange()).isEqualTo(
                "nom: Gamma → Gamma 1, etiqueta: (cap) → \"Refugi\", color: Sense color → Vermell, A la xarxa → Caixa 6");
    }

    @Test
    void savingACardWithoutChangesLogsNothing() {
        int before = campaign.getLegacyLog().size();

        campaign.updateCard(GAMMA, "Gamma", "", "white", CardState.IN_NETWORK);

        assertThat(campaign.getLegacyLog()).hasSize(before);
    }

    @Test
    void severalCardsOfACityAreNumberedAfterTheExistingOnes() {
        campaign.addCard("Lagos 1", "", "#E9B824");
        campaign.addCard("Lagos 3", "", "#E9B824");

        List<CampaignCard> added = campaign.addCards("lagos", 2, "", "#E9B824", CardState.OUT_OF_NETWORK);

        assertThat(added).extracting(CampaignCard::getCityName).containsExactly("lagos 4", "lagos 5");
        assertThat(added).allMatch(card -> card.getState() == CardState.OUT_OF_NETWORK);
        assertThat(lastChange().getChange()).isEqualTo("Carta nova (Fora de la xarxa)");
    }

    @Test
    void twoCardsCannotHaveTheSameName() {
        assertThatThrownBy(() -> campaign.addCard("gamma", "", "white")).hasMessage("Ja hi ha una carta gamma");
        assertThatThrownBy(() -> campaign.updateCard(GAMMA, "Delta", "", "white", CardState.IN_NETWORK))
                .hasMessage("Ja hi ha una carta Delta");
    }

    @Test
    void aCardEnteredByMistakeCanBeDeleted() {
        CampaignCard mistake = campaign.addCard("Kinsassa", "", "white");

        campaign.deleteCard(mistake.getId());

        assertThat(campaign.getCards()).extracting(CampaignCard::getCityName).doesNotContain("Kinsassa");
        assertThat(lastChange().getChange()).isEqualTo("Carta esborrada");
    }

    @Test
    void aCardDrawnInThisGameCannotBeDeleted() {
        assertThatThrownBy(() -> campaign.deleteCard(ALPHA)).hasMessageContaining("no es pot esborrar");
    }

    @Test
    void aNewGamePutsEveryCardBackInTheDeckAndKeepsTheLegacy() {
        campaign.changeState(DELTA, CardState.BOX_6);
        campaign.epidemic(EPSILON);
        campaign.draw(GAMMA);

        campaign.newGame();

        assertThat(campaign.getGame()).isEqualTo(2);
        assertThat(campaign.getPeriods()).isEqualTo(1);
        assertThat(campaign.getHistory()).isEmpty();
        assertThat(campaign.getCards()).allMatch(card -> card.getDrawnInPeriods().isEmpty());
        assertThat(campaign.card(DELTA).getState()).isEqualTo(CardState.BOX_6);
        assertThat(discardPile()).isEmpty();
        assertThat(deckLayers()).containsExactly(Arrays.asList("Alpha", "Beta", "Epsilon", "Gamma"));

        campaign.changeState(DELTA, CardState.IN_NETWORK);
        assertThat(lastChange().getGame()).isEqualTo(2);
    }

    @Test
    void theGridReplacesThePeriodsOfEveryCard() {
        campaign.draw(GAMMA);
        Map<Integer, List<Integer>> drawn = new HashMap<>();
        drawn.put(ALPHA, Arrays.asList(0, 1));
        drawn.put(DELTA, Arrays.asList(1, 7));

        campaign.correct(2, drawn);

        assertThat(campaign.getPeriods()).isEqualTo(2);
        assertThat(campaign.card(ALPHA).getDrawnInPeriods()).containsExactly(0, 1);
        assertThat(campaign.card(BETA).getDrawnInPeriods()).isEmpty();
        assertThat(campaign.card(DELTA).getDrawnInPeriods()).containsExactly(1);
        assertThat(campaign.getHistory()).isEmpty();
        assertThat(discardPile()).containsExactly("Alpha", "Delta");
    }

    @Test
    void cardsFromTheTopLayerFirstAreInOrder() {
        campaign.epidemic(GAMMA);
        campaign.draw(ALPHA);
        campaign.draw(BETA);
        campaign.draw(GAMMA);
        campaign.draw(DELTA);

        assertThat(campaign.getDrawOrderIssues()).isEmpty();
    }

    @Test
    void flagsCardsDrawnBeforeTheTopLayerRanOut() {
        campaign.epidemic(GAMMA);
        campaign.draw(DELTA);
        campaign.draw(EPSILON);

        assertThat(campaign.getDrawOrderIssues()).hasSize(1);
        DrawOrderIssue issue = campaign.getDrawOrderIssues().get(0);
        assertThat(issue.getPeriod()).isEqualTo(1);
        assertThat(issue.getCardIds()).containsExactly(DELTA, EPSILON);
        assertThat(issue.getMessage()).isEqualTo("Ep1: Delta, Epsilon han sortit abans d'acabar les cartes de dalt de "
                + "la pila (les que van sortir a Inicial). Encara hi havia: Alpha, Beta, Gamma.");
    }

    @Test
    void theEpidemicCardComesFromTheBottom() {
        campaign.epidemic(GAMMA);
        campaign.draw(ALPHA);
        campaign.epidemic(DELTA);

        assertThat(campaign.getDrawOrderIssues()).isEmpty();
    }

    @Test
    void onlyOneCardFromTheBottomCanBeTheEpidemicCard() {
        campaign.epidemic(GAMMA);
        campaign.draw(DELTA);
        campaign.epidemic(EPSILON);

        assertThat(campaign.getDrawOrderIssues()).hasSize(1);
        assertThat(campaign.getDrawOrderIssues().get(0).getCardIds()).containsExactly(DELTA, EPSILON);
        assertThat(campaign.getDrawOrderIssues().get(0).getMessage()).endsWith("pot ser la carta de l'epidèmia.");
    }

    @Test
    void cardsOutOfTheGameDoNotNeedToRunOut() {
        campaign.epidemic(GAMMA);
        campaign.changeState(ALPHA, CardState.OUT_OF_NETWORK);
        campaign.draw(BETA);
        campaign.draw(GAMMA);
        campaign.draw(DELTA);

        assertThat(campaign.getDrawOrderIssues()).isEmpty();
    }

    @Test
    void warnsAboutAnInitialInfectionWithoutNineCards() {
        assertThat(campaign.getWarnings()).isEmpty();

        campaign.epidemic(EPSILON);

        assertThat(campaign.getWarnings()).containsExactly("La infecció inicial només té 3 cartes i n'hauria de tenir 9.");
    }

    @Test
    void cardsThatLeftTheNetworkStillCountForTheInitialInfection() {
        Arrays.asList("Eta", "Theta", "Iota", "Kappa", "Lambda", "Mu", "Nu")
                .forEach(name -> campaign.draw(campaign.addCard(name, "", "white").getId()));
        campaign.changeState(ALPHA, CardState.BOX_6);

        campaign.epidemic(EPSILON);

        assertThat(campaign.getWarnings()).isEmpty();
    }

    @Test
    void importsTheThreeSheetColumnsAsOneState() {
        assertThat(CardState.fromSheet(true, false, false)).isEqualTo(CardState.IN_NETWORK);
        assertThat(CardState.fromSheet(false, false, false)).isEqualTo(CardState.OUT_OF_NETWORK);
        assertThat(CardState.fromSheet(true, true, false)).isEqualTo(CardState.END_PILE);
        assertThat(CardState.fromSheet(true, false, true)).isEqualTo(CardState.BOX_6);
        assertThat(CardState.fromSheet(false, true, true)).isEqualTo(CardState.BOX_6);
    }

    private LegacyChange lastChange() {
        return campaign.getLegacyLog().get(campaign.getLegacyLog().size() - 1);
    }

    private InfectionDeckData deckData() {
        return InfectionDeckData.createFromInfectionCardData(campaign.toInfectionCards().stream()
                .filter(InfectionCard::isInTheGame)
                .collect(Collectors.toList()));
    }

    private int epidemicNumber() {
        return deckData().getCurrentEpidemic();
    }

    private List<String> discardPile() {
        return names(deckData().getInfectionCardsInCurrentEpidemic());
    }

    private List<List<String>> deckLayers() {
        return deckData().getPossibleCardsForNextInfectionDraws().stream()
                .map(CampaignTest::names)
                .collect(Collectors.toList());
    }

    private static List<String> names(InfectionSet set) {
        return set.getCardsInEpidemic().stream().map(InfectionCard::getCityName).collect(Collectors.toList());
    }

    private static InfectionCard sheetCard(int id, String name, Boolean... columns) {
        List<Boolean> appearances = new java.util.ArrayList<>();
        appearances.add(true);
        appearances.addAll(Arrays.asList(columns));
        return new InfectionCard(id, name, "", true, false, false, new InfectionCardAppearances(appearances), "white");
    }
}
