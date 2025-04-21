package org.amupoti.pandemic.model;

import lombok.Value;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.reverse;

@Value
public class InfectionDeckData {

    /**
     * List of set of cards which have appeared in each epidemic. One card can appear in multiple epidemics
     */
    List<InfectionSet> infectionSets;
    /**
     * List of set of cards which have appeared in each epidemic. When it appears in a posterior epidemic, it's removed
     * from the original set where they appeared
     */
    List<InfectionSet> remainingCardsInSets;
    int currentEpidemic;


    public InfectionDeckData(List<InfectionSet> infectionSets) {
        this.infectionSets = cloneSets(infectionSets);
        remainingCardsInSets = createInfectionDeckSets(infectionSets);

        //Deck and initial infection are the two initial groups
        currentEpidemic = remainingCardsInSets.size() - 2;
    }

    public static InfectionDeckData createFromInfectionCardData(List<InfectionCard> infectionCards) {

        //The initial
        int numberOfSets = infectionCards.get(0).getNumberOfEpidemics();

        List<InfectionSet> infectionSetList = IntStream.range(0, numberOfSets)
                .mapToObj(currentSet -> InfectionSet.getEpidemicCards(infectionCards, currentSet))
                .collect(Collectors.toList());
        return new InfectionDeckData(infectionSetList);
    }

    private List<InfectionSet> cloneSets(List<InfectionSet> infectionSets) {
        List<InfectionSet> newSets = new LinkedList<>();
        for (InfectionSet infectionSet : infectionSets) {
            newSets.add(new InfectionSet(new LinkedList<>(infectionSet.getCardsInEpidemic())));
        }
        return newSets;
    }

    //remove cards in the last sets so they not appear in previous sets
    private List<InfectionSet> createInfectionDeckSets(List<InfectionSet> infectionSets) {

        int numInfectionSets = infectionSets.size();
        for (int i = numInfectionSets - 1; i >= 0; i--) {
            List<InfectionCard> cardsInEpidemic = infectionSets.get(i).getCardsInEpidemic();
            for (int j = i - 1; j >= 0; j--) {
                List<InfectionCard> previousInfectionSet = infectionSets.get(j).getCardsInEpidemic();
                previousInfectionSet.removeAll(cardsInEpidemic);
            }
        }

        return infectionSets;
    }

    public static String prettyPrint(List<InfectionSet> infectionSets) {
        return infectionSets.stream()
                .filter(infectionSet -> !infectionSet.getCardsInEpidemic().isEmpty())
                .map(infectionSet -> "InfectionSet: " + infectionSet.getCardsInEpidemic().stream()
                        .map(InfectionCard::shortPrint)
                        .collect(Collectors.joining(",")))
                .collect(Collectors.joining("\n"));
    }

    public InfectionSet getInfectionCardsInCurrentEpidemic() {
        return infectionSets.get(infectionSets.size() - 1);
    }

    public List<InfectionSet> getPossibleCardsForNextInfectionDraws() {
        List<InfectionSet> cardsForNextInfection = remainingCardsInSets
                .subList(0, remainingCardsInSets.size() - 1)
                .stream()
                .filter(infectionSet -> !infectionSet.getCardsInEpidemic().isEmpty())
                .collect(Collectors.toList());

        reverse(cardsForNextInfection);

        return cardsForNextInfection;
    }


    public static Map<String, Double> rankCitiesByInfectionRisk(List<InfectionSet> remainingSets, int infectionRate) {
        Map<String, Integer> cityToTotalCount = new HashMap<>();
        Map<String, List<SetDrawInfo>> cityToDraws = new HashMap<>();

        int remaining = infectionRate;

        for (InfectionSet set : remainingSets) {
            List<InfectionCard> cards = set.getCardsInEpidemic();
            int drawCount = Math.min(remaining, cards.size());
            remaining -= drawCount;

            Map<String, Long> cityCountInSet = cards.stream()
                    .map(card -> cityNameWithoutNumber(card.getCityName()))
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            for (Map.Entry<String, Long> entry : cityCountInSet.entrySet()) {
                String city = entry.getKey();
                int countInSet = entry.getValue().intValue();

                cityToTotalCount.merge(city, countInSet, Integer::sum);

                cityToDraws
                        .computeIfAbsent(city, c -> new ArrayList<>())
                        .add(new SetDrawInfo(countInSet, cards.size(), drawCount));
            }

            if (remaining == 0) break;
        }

        Map<String, Double> cityToProbability = new HashMap<>();

        for (String city : cityToDraws.keySet()) {
            List<SetDrawInfo> draws = cityToDraws.get(city);

            // Multiply probabilities of missing in each contributing set
            double probNoHit = 1.0;
            for (SetDrawInfo draw : draws) {
                probNoHit *= hypergeometric(0, draw.cityCardsInSet, draw.totalCardsInSet, draw.cardsDrawnFromSet);
            }

            cityToProbability.put(city, 1.0 - probNoHit);
        }

        List<String> cityNames = remainingSets.stream().flatMap(set -> set.getCardsInEpidemic().stream()).map(InfectionCard::getCityName).collect(Collectors.toList());
        return cityNames.stream().collect(Collectors.toMap(cityName -> cityName, cityName -> cityToProbability.getOrDefault(cityNameWithoutNumber(cityName), 0.0)));

    }

    private static String cityNameWithoutNumber(String cityName) {
        return cityName.replaceAll(" \\d+$", "");
    }

    private static class SetDrawInfo {
        int cityCardsInSet;
        int totalCardsInSet;
        int cardsDrawnFromSet;

        SetDrawInfo(int cityCardsInSet, int totalCardsInSet, int cardsDrawnFromSet) {
            this.cityCardsInSet = cityCardsInSet;
            this.totalCardsInSet = totalCardsInSet;
            this.cardsDrawnFromSet = cardsDrawnFromSet;
        }
    }

    private static double hypergeometric(int k, int K, int N, int n) {
        return binomial(K, k) * binomial(N - K, n - k) / binomial(N, n);
    }

    private static double binomial(int n, int k) {
        if (k > n || k < 0) return 0.0;
        return Math.exp(logFactorial(n) - logFactorial(k) - logFactorial(n - k));
    }

    private static double logFactorial(int n) {
        double result = 0.0;
        for (int i = 2; i <= n; i++) result += Math.log(i);
        return result;
    }



}
