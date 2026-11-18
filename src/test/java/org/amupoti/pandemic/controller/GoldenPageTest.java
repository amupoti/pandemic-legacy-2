package org.amupoti.pandemic.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.amupoti.pandemic.model.InfectionCard;
import org.amupoti.pandemic.service.InfectionModelLoader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Safety net for refactoring: renders the page from a saved copy of the Google sheet and compares everything
 * it shows (status, next epidemic, every pile with its cards, labels, appearances and percentages, and the discard
 * pile) with a reviewed snapshot.
 * <p>
 * The saved sheet is imported into a new campaign, as a player would do the first time.
 * Input: {@value #SHEET}. Expected page: {@value #EXPECTED}.
 * When a change is meant to alter what the page shows, regenerate the snapshot with
 * {@code mvn test -Dtest=GoldenPageTest -Dgolden.update=true} and review the diff in git.
 */
@SpringBootTest(properties = "pandemic.campaign-file=target/golden-test/campaign.json")
@AutoConfigureMockMvc
class GoldenPageTest {

    private static final String SHEET = "golden/sheet-2026-11-18.json";
    private static final String EXPECTED = "golden/expected-page-2026-11-18.json";
    private static final Path EXPECTED_SOURCE = Paths.get("src/test/resources", EXPECTED);

    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Autowired
    private MockMvc mvc;

    @TestConfiguration
    static class SavedSheet {
        @Bean
        @Primary
        InfectionModelLoader savedSheetLoader() {
            return new InfectionModelLoader() {
                @Override
                public List<InfectionCard> loadAllFromGooglesheet(String spreadsheetId) throws IOException {
                    try (InputStream in = getClass().getClassLoader().getResourceAsStream(SHEET)) {
                        List<List<Object>> rows = JSON.readValue(in, new TypeReference<List<List<Object>>>() {
                        });
                        return loadAllFromRows(rows);
                    }
                }
            };
        }
    }

    @Test
    void thePageShowsTheSameAsTheReviewedSnapshot() throws Exception {
        Files.deleteIfExists(Paths.get("target/golden-test/campaign.json"));
        mvc.perform(post("/import"));

        byte[] body = mvc.perform(get("/")).andReturn().getResponse().getContentAsByteArray();
        String html = new String(body, StandardCharsets.UTF_8);
        String actual = JSON.writeValueAsString(describe(Jsoup.parse(html))) + "\n";

        if (Boolean.getBoolean("golden.update") || !Files.exists(EXPECTED_SOURCE)) {
            Files.write(EXPECTED_SOURCE, actual.getBytes(StandardCharsets.UTF_8));
        }

        String expected = new String(Files.readAllBytes(EXPECTED_SOURCE), StandardCharsets.UTF_8);
        assertThat(actual)
                .as("La pàgina ha canviat respecte a %s. Si el canvi és volgut, regenera-la amb -Dgolden.update=true", EXPECTED)
                .isEqualTo(expected);
    }

    /**
     * Turns the rendered page into plain data, relying only on the CSS classes of the page and not on its layout
     */
    private static Map<String, Object> describe(Document page) {
        Map<String, Object> result = new LinkedHashMap<>();

        List<String> status = page.select(".epidemic-status .epidemic-item strong").eachText();
        Map<String, Object> statusData = new LinkedHashMap<>();
        statusData.put("epidemic", status.get(0));
        statusData.put("infectionRate", status.get(1));
        statusData.put("turnsSinceLastEpidemic", status.get(2));
        result.put("status", statusData);

        result.put("nextEpidemic", page.select(".epidemic-risk-city").stream()
                .map(city -> {
                    Map<String, Object> risk = new LinkedHashMap<>();
                    risk.put("city", city.selectFirst(".epidemic-risk-name").text());
                    risk.put("probability", city.selectFirst("strong").text());
                    return risk;
                })
                .collect(Collectors.toList()));

        result.put("piles", page.select(".section-header").stream()
                .map(GoldenPageTest::describePile)
                .collect(Collectors.toList()));
        return result;
    }

    private static Map<String, Object> describePile(Element header) {
        Element pile = header.parent();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("title", header.selectFirst(".title").text());
        data.put("count", header.selectFirst(".card-count-inline").text());
        data.put("cards", pile.select(".Card").stream()
                .map(GoldenPageTest::describeCard)
                .collect(Collectors.toList()));
        return data;
    }

    private static Map<String, Object> describeCard(Element card) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", card.selectFirst(".card-name").text());
        data.put("label", textOf(card, ".card-label"));
        data.put("appearances", textOf(card, ".card-times"));
        data.put("probability", textOf(card, ".card-prob"));
        return data;
    }

    private static String textOf(Element parent, String selector) {
        Element element = parent.selectFirst(selector);
        return element == null ? null : element.text();
    }
}
