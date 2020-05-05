package com.example.sequencemultiplayer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Deck {

    String deck[];

    final static int totalCards = 104;

    Deck() {
        deck = new String[]{"01 73",
                            "02 72",
                            "03 62",
                            "04 52",
                            "05 42",
                            "06 32",
                            "07 22",
                            "08 23",
                            "10 74",
                            "11 44",
                            "12 45",
                            "13 98",
                            "14 97",
                            "15 96",
                            "16 95",
                            "17 94",
                            "18 93",
                            "19 24",
                            "20 75",
                            "21 54",
                            "25 29",
                            "26 39",
                            "27 49",
                            "28 92",
                            "30 76",
                            "31 55",
                            "33 82",
                            "34 81",
                            "35 71",
                            "36 61",
                            "37 59",
                            "38 91",
                            "40 77",
                            "41 56",
                            "43 83",
                            "46 51",
                            "47 69",
                            "48 80",
                            "50 78",
                            "53 84",
                            "57 79",
                            "58 70",
                            "60 68",
                            "63 85",
                            "64 86",
                            "65 87",
                            "66 88",
                            "67 89",
                            "01 73",
                            "02 72",
                            "03 62",
                            "04 52",
                            "05 42",
                            "06 32",
                            "07 22",
                            "08 23",
                            "10 74",
                            "11 44",
                            "12 45",
                            "13 98",
                            "14 97",
                            "15 96",
                            "16 95",
                            "17 94",
                            "18 93",
                            "19 24",
                            "20 75",
                            "21 54",
                            "25 29",
                            "26 39",
                            "27 49",
                            "28 92",
                            "30 76",
                            "31 55",
                            "33 82",
                            "34 81",
                            "35 71",
                            "36 61",
                            "37 59",
                            "38 91",
                            "40 77",
                            "41 56",
                            "43 83",
                            "46 51",
                            "47 69",
                            "48 80",
                            "50 78",
                            "53 84",
                            "57 79",
                            "58 70",
                            "60 68",
                            "63 85",
                            "64 86",
                            "65 87",
                            "66 88",
                            "67 89",
                            "100 101",
                            "100 101",
                            "101 100",
                            "101 100",
                            "110 111",
                            "110 111",
                            "111 110",
                            "111 110"};
        shuffleDeck();
    }

    void shuffleDeck() {
        for(int i=0; i<10; i++) {
            shuffleCollection();
            shuffleManual();
        }
    }

    void shuffleManual() {
        int n = deck.length;
        Random random = new Random();
        random.nextInt();
        for (int i = 0; i < n; i++) {
            int change = i + random.nextInt(n - i);
            swap(i, change);
        }
    }

    void shuffleCollection() {
        List<String> shuffledDeck = Arrays.asList(deck);
        Collections.shuffle(Arrays.asList(shuffledDeck));
        shuffledDeck.toArray(deck);
    }

    void swap(int i, int change) {
        String helper = deck[i];
        deck[i] = deck[change];
        deck[change] = helper;
    }

}
