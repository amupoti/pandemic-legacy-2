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
            "Denver", "Chicago", "San Petersburgo", "Johannesburgo", "Montreal", "Essen", "Milan", "Atlanta", "Madrid", "");
    private static final List<String> blackCities = Arrays.asList(
            "El Cairo", "Estambul", "Antananarivo", "Tripoli", "Moscú", "Bagdad", "Bombay", "Teheran", "Riad", "Calcuta",
            "Argel", "Karachi", "Nueva Delhi");
    private static final List<String> yellowCities = Arrays.asList(
            "Sao Paulo", "Lima", "Dar es-Salam", "Lagos", "Santiago",
            "Ciudad de Mexico", "Jacksonville", "Jartum", "Buenos Aires", "Los Angeles",
            "Bogota", "Kinsasa", "Miami", "Los Angeles", "Johannesburgo (SA)", "Madras");
    private static final List<String> redCities = Arrays.asList("Hong Kong", "Sidney", "Bangkok", "Shangai", "Manila",
            "Seul", "Ho Chi Minh", "Taipei", "Osaka", "Pekin", "Tokio", "Yakarta");

    public static final String LIGHT_BLUE = "#9999f9";
    public static final String GREY = "#c7c5c5";
    public static final String YELLOW = "#f3f3a3";
    public static final String LIGHT_GREEN = "#b3ffb3";
    private static final String RED = "#ffcccc";

    /**
     * Row structure in the google sheet is the following:
     */
    public static InfectionCard fromRow(List<Object> row) {

        log.info(Arrays.toString(row.toArray()));
        String cityName = (String) row.get(0);
        String label = buildLabel((String) row.get(1));
        Boolean inNetwork = parseBoolean(row.get(2));
        Boolean destroyed = parseBoolean(row.get(3));
        Boolean inBox6 = parseBoolean(row.get(4));
        String color = getColorForCityName(cityName);

        InfectionCardAppearances infectionCardAppearances = InfectionCardAppearances.fromRow(row);
        return new InfectionCard(cityName, label, inNetwork, destroyed, inBox6, infectionCardAppearances, color);
    }

    private static String buildLabel(String label) {
        if (label != null && !label.isEmpty()) return "(" + label + ")";
        else return label;
    }

    private static String getColorForCityName(String cityName) {
        String cityWithoutDigits = cityName.replaceAll("\\d", "").trim();
        if (blueCities.contains(cityWithoutDigits)) return LIGHT_BLUE;
        if (blackCities.contains(cityWithoutDigits)) return GREY;
        if (yellowCities.contains(cityWithoutDigits)) return YELLOW;
        if (redCities.contains(cityWithoutDigits)) return RED;
        if (cityName.contains("Desalmados")) return LIGHT_GREEN;
        return "white";
    }


}
