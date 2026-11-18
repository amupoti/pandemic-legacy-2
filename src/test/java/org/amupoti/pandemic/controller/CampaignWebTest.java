package org.amupoti.pandemic.controller;

import org.amupoti.pandemic.campaign.Campaign;
import org.amupoti.pandemic.campaign.CampaignService;
import org.amupoti.pandemic.model.InfectionCard;
import org.amupoti.pandemic.model.InfectionCardAppearances;
import org.amupoti.pandemic.service.InfectionModelLoader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Plays through the pages with a fake Google sheet: the initial infection drew Alpha and Beta,
 * Gamma, Delta and Epsilon have never been drawn and Zeta is destroyed.
 */
@SpringBootTest(properties = "pandemic.campaign-file=target/web-test/campaign.json")
@AutoConfigureMockMvc
class CampaignWebTest {

    private static final int ALPHA = 0;
    private static final int GAMMA = 2;
    private static final int EPSILON = 4;
    private static final int ZETA = 5;

    static boolean sheetUnavailable;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CampaignService campaignService;

    @Value("${pandemic.campaign-file}")
    private String campaignFile;

    @TestConfiguration
    static class FakeSheet {
        @Bean
        @Primary
        InfectionModelLoader fakeSheetLoader() {
            return new InfectionModelLoader() {
                @Override
                public List<InfectionCard> loadAllFromGooglesheet(String spreadsheetId) throws IOException {
                    if (sheetUnavailable) throw new IOException("sense connexió");
                    return Arrays.asList(
                            card(0, "Alpha", true, true, false),
                            card(1, "Beta", true, true, false),
                            card(2, "Gamma", false, true, false),
                            card(3, "Delta", false, true, false),
                            card(4, "Epsilon", false, true, false),
                            card(5, "Zeta", false, false, true));
                }
            };
        }
    }

    @BeforeEach
    void startWithoutCampaign() throws IOException {
        sheetUnavailable = false;
        Files.deleteIfExists(Paths.get(campaignFile));
    }

    @Test
    void withoutCampaignTheSetupPageOffersTheImport() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("setup"))
                .andExpect(content().string(containsString("Importar del Google Sheets")));
    }

    @Test
    void importingShowsTheDeckWithoutTheDestroyedCard() throws Exception {
        mvc.perform(post("/import"))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("message", "Campanya importada: 6 cartes."));

        mvc.perform(get("/"))
                .andExpect(view().name("predict"))
                .andExpect(content().string(containsString("Robar Gamma")))
                .andExpect(content().string(not(containsString("Zeta"))))
                .andExpect(content().string(containsString("Propera epidèmia")));
    }

    @Test
    void aFailedImportExplainsWhy() throws Exception {
        sheetUnavailable = true;

        mvc.perform(post("/import"))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", containsString("sense connexió")));
        assertThat(campaignService.current()).isEmpty();
    }

    @Test
    void drawingACardMovesItToTheDiscardPile() throws Exception {
        mvc.perform(post("/import"));

        mvc.perform(post("/draw/" + GAMMA)).andExpect(redirectedUrl("/"));

        assertThat(campaign().card(GAMMA).getDrawnInPeriods()).containsExactly(0);
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("Robada: Gamma")))
                .andExpect(content().string(containsString("Desfer")));
    }

    @Test
    void drawingACardTwiceShowsAnError() throws Exception {
        mvc.perform(post("/import"));

        mvc.perform(post("/draw/" + ALPHA))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", "Alpha ja és a la pila de descarts"));
    }

    @Test
    void anEpidemicStartsANewPeriodWithAnEmptyDiscardPile() throws Exception {
        mvc.perform(post("/import"));
        mvc.perform(get("/").param("mode", "epidemia"))
                .andExpect(content().string(containsString("toca la carta que has tret")))
                .andExpect(content().string(containsString("Epidèmia a Epsilon")));

        mvc.perform(post("/epidemic/" + EPSILON)).andExpect(redirectedUrl("/"));

        assertThat(campaign().getPeriods()).isEqualTo(2);
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Encara no s'ha robat cap carta")))
                .andExpect(content().string(containsString("Epidèmia a Epsilon")));
    }

    @Test
    void aWrongEpidemicCardKeepsTheEpidemicMode() throws Exception {
        mvc.perform(post("/import"));

        mvc.perform(post("/epidemic/" + ALPHA))
                .andExpect(redirectedUrl("/?mode=epidemia"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void undoRevertsTheLastAction() throws Exception {
        mvc.perform(post("/import"));
        mvc.perform(post("/epidemic/" + EPSILON));

        mvc.perform(post("/undo")).andExpect(redirectedUrl("/"));

        assertThat(campaign().getPeriods()).isEqualTo(1);
        assertThat(campaign().getHistory()).isEmpty();
    }

    @Test
    void theCampaignPageListsEveryCard() throws Exception {
        mvc.perform(post("/import"));

        mvc.perform(get("/campanya"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("5 cartes a la pila d&#39;infecció de 6")))
                .andExpect(content().string(containsString("Zeta")));
    }

    @Test
    void aLegacyChangeTakesACardOutOfTheGame() throws Exception {
        mvc.perform(post("/import"));

        mvc.perform(post("/campanya/cards/" + GAMMA)
                        .param("cityName", "Gamma")
                        .param("label", " Bloqueig ")
                        .param("color", "#E74C3C")
                        .param("state", "END_PILE"))
                .andExpect(redirectedUrl("/campanya#card-" + GAMMA));

        assertThat(campaign().card(GAMMA).getLabel()).isEqualTo("Bloqueig");
        assertThat(campaign().card(GAMMA).isInTheGame()).isFalse();
        mvc.perform(get("/")).andExpect(content().string(not(containsString("Robar Gamma"))));
    }

    @Test
    void aDestroyedCardCanComeBack() throws Exception {
        mvc.perform(post("/import"));

        mvc.perform(post("/campanya/cards/" + ZETA).param("color", "white").param("state", "IN_NETWORK"));

        mvc.perform(get("/")).andExpect(content().string(containsString("Robar Zeta")));
    }

    @Test
    void aNewCardStartsAtTheBottomOfTheDeck() throws Exception {
        mvc.perform(post("/import"));

        mvc.perform(post("/campanya/cards").param("cityName", "Kinsasa").param("color", "#E9B824"))
                .andExpect(flash().attribute("message", "Cartes afegides al fons de la pila: Kinsasa."));

        assertThat(campaign().getCards()).hasSize(7);
        mvc.perform(get("/")).andExpect(content().string(containsString("Robar Kinsasa")));
    }

    @Test
    void reimportingNeedsConfirmation() throws Exception {
        mvc.perform(post("/import"));
        mvc.perform(post("/draw/" + GAMMA));

        mvc.perform(post("/campanya/import")).andExpect(flash().attributeExists("error"));
        assertThat(campaign().getHistory()).hasSize(1);

        mvc.perform(post("/campanya/import").param("confirm", "true")).andExpect(flash().attributeExists("message"));
        assertThat(campaign().getHistory()).isEmpty();
    }

    @Test
    void actionsWithoutCampaignShowAnError() throws Exception {
        mvc.perform(post("/draw/" + GAMMA))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("error", containsString("Comença-la")));
    }

    @Test
    void severalCardsCanBeAddedOutOfTheNetwork() throws Exception {
        mvc.perform(post("/import"));

        mvc.perform(post("/campanya/cards").param("cityName", "Lagos").param("count", "2")
                        .param("color", "#E9B824").param("state", "OUT_OF_NETWORK"))
                .andExpect(flash().attribute("message", "Cartes afegides (Fora de la xarxa): Lagos 1, Lagos 2."));

        mvc.perform(get("/")).andExpect(content().string(not(containsString("Robar Lagos"))));
        mvc.perform(get("/campanya")).andExpect(content().string(containsString("Carta nova (Fora de la xarxa)")));
    }

    @Test
    void aCardEnteredByMistakeCanBeDeleted() throws Exception {
        mvc.perform(post("/import"));
        mvc.perform(post("/campanya/cards").param("cityName", "Kinsassa").param("color", "white"));
        int mistake = campaign().getCards().get(campaign().getCards().size() - 1).getId();

        mvc.perform(post("/campanya/cards/" + mistake + "/delete")).andExpect(flash().attribute("message", "Carta esborrada."));

        assertThat(campaign().getCards()).hasSize(6);
    }

    @Test
    void aNewGameNeedsConfirmationAndStartsTheInitialInfection() throws Exception {
        mvc.perform(post("/import"));
        mvc.perform(post("/epidemic/" + EPSILON));

        mvc.perform(post("/campanya/new-game")).andExpect(flash().attributeExists("error"));
        assertThat(campaign().getGame()).isEqualTo(1);

        mvc.perform(post("/campanya/new-game").param("confirm", "true")).andExpect(redirectedUrl("/"));

        assertThat(campaign().getGame()).isEqualTo(2);
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("Infecció inicial: 0 de 9 cartes.")))
                .andExpect(content().string(containsString("Robar Alpha")));
    }

    @Test
    void theGridShowsTheSheetAndSavesCorrections() throws Exception {
        mvc.perform(post("/import"));
        String grid = mvc.perform(get("/campanya/graella"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Zeta")))
                .andReturn().getResponse().getContentAsString();
        Document page = Jsoup.parse(grid);
        assertThat(page.selectFirst("input[name=c" + ALPHA + "p0]").hasAttr("checked")).isTrue();
        assertThat(page.selectFirst("input[name=c" + GAMMA + "p0]").hasAttr("checked")).isFalse();

        mvc.perform(post("/campanya/graella").param("periods", "2")
                        .param("c" + GAMMA + "p0", "1")
                        .param("c" + EPSILON + "p1", "1"))
                .andExpect(redirectedUrl("/campanya/graella"))
                .andExpect(flash().attribute("message", "Correccions desades."));

        assertThat(campaign().getPeriods()).isEqualTo(2);
        assertThat(campaign().card(ALPHA).getDrawnInPeriods()).isEmpty();
        assertThat(campaign().card(GAMMA).getDrawnInPeriods()).containsExactly(0);
        assertThat(campaign().card(EPSILON).getDrawnInPeriods()).containsExactly(1);
    }

    @Test
    void warningsAreShownOnThePlayPage() throws Exception {
        mvc.perform(post("/import"));
        mvc.perform(post("/epidemic/" + EPSILON));

        mvc.perform(get("/")).andExpect(content().string(containsString("La infecció inicial només té 3 cartes")));
    }

    @Test
    void theCampaignCanBeExportedAndRestored() throws Exception {
        mvc.perform(post("/import"));
        byte[] exported = mvc.perform(get("/campanya/export"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        mvc.perform(post("/draw/" + GAMMA));

        MockMultipartFile file = new MockMultipartFile("file", "campanya.json", "application/json", exported);
        mvc.perform(multipart("/campanya/restore").file(file)).andExpect(flash().attributeExists("error"));
        assertThat(campaign().getHistory()).hasSize(1);

        mvc.perform(multipart("/campanya/restore").file(file).param("confirm", "true"))
                .andExpect(flash().attribute("message", "Campanya restaurada des de campanya.json."));
        assertThat(campaign().getHistory()).isEmpty();
    }

    @Test
    void restoringAWrongFileKeepsTheCampaign() throws Exception {
        mvc.perform(post("/import"));
        MockMultipartFile file = new MockMultipartFile("file", "notes.json", "application/json", "{}".getBytes());

        mvc.perform(multipart("/campanya/restore").file(file).param("confirm", "true"))
                .andExpect(flash().attribute("error", "El fitxer no té cap carta"));
        assertThat(campaign().getCards()).hasSize(6);
    }

    @Test
    void aCampaignCanStartFromScratchWithoutTheSheet() throws Exception {
        mvc.perform(get("/")).andExpect(content().string(containsString("Començar de zero")));

        mvc.perform(post("/new-campaign")).andExpect(redirectedUrl("/campanya#afegir"));
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("predict"));

        mvc.perform(post("/campanya/cards").param("cityName", "Lagos").param("count", "3").param("color", "#E9B824"));
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("Robar Lagos 3")))
                .andExpect(content().string(containsString("Infecció inicial: 0 de 9 cartes.")));
    }

    @Test
    void aSecondEmptyCampaignIsRefused() throws Exception {
        mvc.perform(post("/import"));

        mvc.perform(post("/new-campaign")).andExpect(flash().attribute("error", "Ja hi ha una campanya."));
        assertThat(campaign().getCards()).hasSize(6);
    }

    @Test
    void aCampaignCanStartFromAFile() throws Exception {
        mvc.perform(post("/import"));
        byte[] exported = mvc.perform(get("/campanya/export")).andReturn().getResponse().getContentAsByteArray();
        Files.delete(Paths.get(campaignFile));
        mvc.perform(get("/")).andExpect(view().name("setup"));

        MockMultipartFile file = new MockMultipartFile("file", "campanya.json", "application/json", exported);
        mvc.perform(multipart("/campanya/restore").file(file).param("confirm", "true"));

        assertThat(campaign().getCards()).hasSize(6);
    }

    private Campaign campaign() {
        return campaignService.current().orElseThrow(AssertionError::new);
    }

    private static InfectionCard card(int id, String name, boolean drawnInInitialInfection, boolean inNetwork, boolean destroyed) {
        return new InfectionCard(id, name, "", inNetwork, destroyed, false,
                new InfectionCardAppearances(Arrays.asList(true, drawnInInitialInfection)), "white");
    }
}
