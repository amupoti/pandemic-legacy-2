package org.amupoti.pandemic.service;

import lombok.extern.slf4j.Slf4j;
import org.amupoti.pandemic.model.InfectionCard;
import org.amupoti.pandemic.model.InfectionCardAppearances;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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

    public static final String LIGHT_BLUE = "#2FA4CF";
    public static final String GREY = "#3B3B3B";
    public static final String YELLOW = "#E9B824";
    public static final String LIGHT_GREEN = "#b3ffb3";
    public static final String RED = "#E74C3C";

    /**
     * Row structure in the google sheet is the following:
     */
    public static InfectionCard fromRow(int id, List<Object> row) {

        log.info(Arrays.toString(row.toArray()));
        String cityName = (String) row.get(0);
        String label = (String) row.get(1);
        Boolean inNetwork = parseBoolean(row.get(2));
        Boolean destroyed = parseBoolean(row.get(3));
        Boolean inBox6 = parseBoolean(row.get(4));
        String color = getColorForCityName(cityName);

        InfectionCardAppearances infectionCardAppearances = InfectionCardAppearances.fromRow(row);
        return new InfectionCard(id, cityName, label, inNetwork, destroyed, inBox6, infectionCardAppearances, color);
    }

    private static String getColorForCityName(String cityName) {
        String city = normalize(cityName.replaceAll("\\d", ""));
        if (containsCity(blueCities, city)) return LIGHT_BLUE;
        if (containsCity(blackCities, city)) return GREY;
        if (containsCity(yellowCities, city)) return YELLOW;
        if (containsCity(redCities, city)) return RED;
        if (cityName.contains("Desalmados")) return LIGHT_GREEN;
        return "white";
    }

    private static boolean containsCity(List<String> cities, String normalizedCity) {
        return cities.stream().map(InfectionCardFactory::normalize).anyMatch(normalizedCity::equals);
    }

    /**
     * Ignores accents, case and extra spaces, so "Ciudad de México" matches "Ciudad de Mexico"
     */
    private static String normalize(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
