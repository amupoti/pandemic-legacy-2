package org.amupoti.pandemic.campaign;

/**
 * Where a card is in the campaign. Only cards in the network are part of the infection deck. Legacy changes can move
 * a card from any state to any other, in both directions.
 */
public enum CardState {
    IN_NETWORK("A la xarxa"),
    OUT_OF_NETWORK("Fora de la xarxa"),
    END_PILE("Pila final partida"),
    BOX_6("Caixa 6");

    private final String description;

    CardState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Reads the three columns of the Google sheet. Box 6 wins over the end pile, and both win over the network column
     */
    public static CardState fromSheet(boolean inNetwork, boolean endPile, boolean inBox6) {
        if (inBox6) return BOX_6;
        if (endPile) return END_PILE;
        return inNetwork ? IN_NETWORK : OUT_OF_NETWORK;
    }
}
