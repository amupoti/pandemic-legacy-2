package org.amupoti.pandemic.campaign;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * One infection card of the campaign, with the infection periods in which it has been drawn.
 * Period 0 is the initial infection, period 1 runs from the first epidemic to the second, and so on.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCard {

    private int id;
    private String cityName;
    private String label;
    private String color;
    private CardState state = CardState.IN_NETWORK;
    private SortedSet<Integer> drawnInPeriods = new TreeSet<>();

    @JsonIgnore
    public boolean isInTheGame() {
        return state == CardState.IN_NETWORK;
    }
}
