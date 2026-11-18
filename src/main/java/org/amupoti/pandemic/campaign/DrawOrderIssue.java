package org.amupoti.pandemic.campaign;

import lombok.Value;

import java.util.Set;

/**
 * Cards marked in a period that, by the order of the deck, could not have been drawn yet
 */
@Value
public class DrawOrderIssue {
    /**
     * Period with the problem: 1 is the period after the first epidemic
     */
    int period;
    /**
     * Cards drawn too early, to mark them in the grid
     */
    Set<Integer> cardIds;
    String message;
}
