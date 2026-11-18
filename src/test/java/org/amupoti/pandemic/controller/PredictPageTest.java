package org.amupoti.pandemic.controller;

import org.amupoti.pandemic.model.InfectionCard;
import org.amupoti.pandemic.model.InfectionCardAppearances;
import org.amupoti.pandemic.service.InfectionModelLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renders the page with a fake sheet just after the first epidemic: no card has been drawn since,
 * so the discard pile is empty.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PredictPageTest {

    @Autowired
    private MockMvc mvc;

    @TestConfiguration
    static class FakeSheet {
        @Bean
        @Primary
        InfectionModelLoader fakeSheetLoader() {
            return new InfectionModelLoader() {
                @Override
                public List<InfectionCard> loadFromGooglesheet(String spreadsheetId) {
                    return Arrays.asList(
                            card("Alpha", true, true, false),
                            card("Beta", true, false, false),
                            card("Gamma", true, false, false));
                }
            };
        }
    }

    @Test
    void theDiscardPileCanBeEmptyRightAfterAnEpidemic() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Encara no s'ha robat cap carta")))
                .andExpect(content().string(containsString("Gamma")));
    }

    private static InfectionCard card(String name, Boolean... appearances) {
        return new InfectionCard(name, "", true, false, false,
                new InfectionCardAppearances(Arrays.asList(appearances)), "white");
    }
}
