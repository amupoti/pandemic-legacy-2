package org.amupoti.pandemic.service;

import lombok.extern.slf4j.Slf4j;
import org.amupoti.pandemic.model.InfectionCard;
import org.amupoti.pandemic.model.InfectionCardAppearances;

import java.util.Arrays;
import java.util.List;

import static org.amupoti.pandemic.util.SheetsServiceUtil.parseBoolean;

@Slf4j
public class InfectionCardFactory {

    private static final List<String> blueCities = Arrays.asList(
            "San Francisco", "New York", "Washington", "Frankfurt", "Londres", "Paris",
            "Denver", "Chicago", "San Petersburgo", "Johannesburgo");
    private static final List<String> blackCities = Arrays.asList(
            "El Cairo", "Istambul", "Antananarivo", "Tripoli", "Moscú");
    private static final List<String> yellowCities = Arrays.asList(
            "Sao Paulo", "Lima", "Dar es-Salam", "Lagos", "Santiago",
            "Ciudad de Mexico", "Jacksonville", "Jartum", "Buenos Aires", "Los Angeles");

    /**
     * Row structure in the google sheet is the following:
     */
    public static InfectionCard fromRow(List<Object> row) {

        log.info(Arrays.toString(row.toArray()));
        String cityName = (String) row.get(0);
        String label = (String) row.get(1);
        Boolean inNetwork = parseBoolean(row.get(2));
        Boolean destroyed = parseBoolean(row.get(3));
        Boolean inBox6 = parseBoolean(row.get(4));
        String color = getColorForCityName(cityName);

        InfectionCardAppearances infectionCardAppearances = InfectionCardAppearances.fromRow(row);
        return new InfectionCard(cityName, label, inNetwork, destroyed, inBox6, infectionCardAppearances, color);
    }

    private static String getColorForCityName(String cityName) {
        String cityWithoutDigits = cityName.replaceAll("\\d", "").trim();
        if (blueCities.contains(cityWithoutDigits)) return "#9999f9";
        if (blackCities.contains(cityWithoutDigits)) return "#c7c5c5";
        if (yellowCities.contains(cityWithoutDigits)) return "#f3f3a3";
        return "white";
    }


}
