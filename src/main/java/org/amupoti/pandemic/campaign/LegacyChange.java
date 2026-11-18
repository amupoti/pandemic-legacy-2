package org.amupoti.pandemic.campaign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One change to a card between or during games, kept so the history of the campaign can be read back
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegacyChange {
    private int game;
    private String card;
    private String change;
}
