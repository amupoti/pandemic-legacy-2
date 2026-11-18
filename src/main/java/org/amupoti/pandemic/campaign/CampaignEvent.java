package org.amupoti.pandemic.campaign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An action taken during play, kept so it can be undone
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignEvent {

    public enum Type {
        /** A card drawn from the top of the infection deck during the infection step */
        DRAW,
        /** An epidemic: the card drawn from the bottom of the deck */
        EPIDEMIC
    }

    private Type type;
    private int cardId;
}
