package net.micaxs.slotmachine.utils;

public class DeckHandler {

    private static final String[] VALUES = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K" };
    private static final String[] SUITS = { "H", "D", "C", "S" };
    private static final String[] DECK = new String[52];

    public static void DeckHandler() {
        createDeck();
        shuffleDeck();
    }

    public static void createDeck() {
        int index = 0;
        for (String value : VALUES) {
            for (String suit : SUITS) {
                DECK[index] = suit + value;
                index++;
            }
        }
    }

    public static void shuffleDeck() {
        for (int i = 0; i < DECK.length; i++) {
            int randomIndex = (int) (Math.random() * DECK.length);
            String temp = DECK[i];
            DECK[i] = DECK[randomIndex];
            DECK[randomIndex] = temp;
        }
    }

    public static String[] getDeck() {
        return DECK;
    }

    public static String drawCard() {
        String card = DECK[0];
        for (int i = 0; i < DECK.length - 1; i++) {
            DECK[i] = DECK[i + 1];
        }
        DECK[DECK.length - 1] = null;
        return card;
    }

    public static void burnCard() {
        for (int i = 0; i < DECK.length - 1; i++) {
            DECK[i] = DECK[i + 1];
        }
        DECK[DECK.length - 1] = null;
    }

    public static void resetDeck() {
        createDeck();
        shuffleDeck();
    }

    public static int getDeckSize() {
        int size = 0;
        for (String card : DECK) {
            if (card != null) {
                size++;
            }
        }
        return size;
    }
}
