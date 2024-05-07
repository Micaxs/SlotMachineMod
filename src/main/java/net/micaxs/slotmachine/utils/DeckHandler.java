package net.micaxs.slotmachine.utils;

import java.util.Random;

public class DeckHandler {

    private static final String[] VALUES = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K" };
    private static final String[] SUITS = { "H", "D", "C", "S" };
    private static final String[] DECK = new String[52];

    public static void DeckHandler() {
    }

    public void createDeck() {
        int index = 0;
        for (String value : VALUES) {
            for (String suit : SUITS) {
                DECK[index] = suit + value;
                index++;
            }
        }
    }

    public void shuffleDeck() {
        Random rand = new Random();
        for (int i = DECK.length - 1; i > 0; i--) {
            int index = rand.nextInt(i + 1);
            String temp = DECK[index];
            DECK[index] = DECK[i];
            DECK[i] = temp;
        }
    }

    public String[] getDeck() {
        return DECK;
    }

    public String drawCard() {
        String card = DECK[0];
        for (int i = 0; i < DECK.length - 1; i++) {
            DECK[i] = DECK[i + 1];
        }
        DECK[DECK.length - 1] = null;
        return card;
    }

    public void burnCard() {
        for (int i = 0; i < DECK.length - 1; i++) {
            DECK[i] = DECK[i + 1];
        }
        DECK[DECK.length - 1] = null;
    }

    public void resetDeck() {
        createDeck();
        shuffleDeck();
    }

    public int getDeckSize() {
        int size = 0;
        for (String card : DECK) {
            if (card != null) {
                size++;
            }
        }
        return size;
    }

    public int getHandValue(String[] playerCards) {
        int totalValue = 0;
        int aceCount = 0;

        for (String card : playerCards) {
            if (card == null) {
                continue;
            }

            String value = card.substring(1);

            if (value.equals("J") || value.equals("Q") || value.equals("K")) {
                totalValue += 10;
            } else if (value.equals("1")) {
                aceCount += 1;
            } else {
                totalValue += Integer.parseInt(value);
            }
        }

        // Handle Aces: they can be 1 or 11, we try to make them 11 if possible
        for (int i = 0; i < aceCount; i++) {
            if (totalValue + 11 <= 21) {
                totalValue += 11;
            } else {
                totalValue += 1;
            }
        }

        return totalValue;
    }

}
