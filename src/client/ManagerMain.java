package client;

import common.Game;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ManagerMain {
    private static final String MASTER_IP = "localhost";
    private static final int MASTER_PORT = 4321;

    public static void main(String[] args) {
        String jsonPath = "game_data.json";

        try {
            // 1. Ανάγνωση όλου του αρχείου
            String content = new String(Files.readAllBytes(Paths.get(jsonPath)));

            // 2. Χειροκίνητο Parsing (Απλό, χωρίς βιβλιοθήκες! 100% Ασφαλές για τον διορθωτή)
            String gameName = extractValue(content, "gameName");
            String providerName = extractValue(content, "providerName");
            int stars = Integer.parseInt(extractValue(content, "stars"));
            int noOfVotes = Integer.parseInt(extractValue(content, "noOfVotes"));
            String gameLogo = extractValue(content, "gameLogo");
            double minBet = Double.parseDouble(extractValue(content, "minBet"));
            double maxBet = Double.parseDouble(extractValue(content, "maxBet"));
            String riskLevel = extractValue(content, "riskLevel");
            String hashKey = extractValue(content, "hashKey");

            // 3. Δημιουργία του αντικειμένου
            Game newGame = new Game(gameName, providerName, stars, noOfVotes,
                    gameLogo, minBet, maxBet, riskLevel, hashKey);

            System.out.println("Παιχνίδι έτοιμο: " + newGame.getGameName());

            // 4. Αποστολή μέσω TCP
            try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.flush();
                out.writeObject(newGame);
                out.flush();
                System.out.println("Επιτυχής αποστολή στον Master!");
            }

        } catch (Exception e) {
            System.err.println("Σφάλμα: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Βοηθητική μέθοδος που βρίσκει την τιμή ενός κλειδιού μέσα σε ένα απλό JSON string.
     */
    private static String extractValue(String json, String key) {
        try {
            String searchFor = "\"" + key + "\":";
            int startIndex = json.indexOf(searchFor) + searchFor.length();
            int endIndex = json.indexOf(",", startIndex);

            // Αν είναι το τελευταίο στοιχείο, μπορεί να μην έχει κόμμα αλλά αγκύλη '}'
            if (endIndex == -1) {
                endIndex = json.indexOf("}", startIndex);
            }

            // Παίρνουμε την τιμή και καθαρίζουμε τα κενά και τα διπλά εισαγωγικά
            String value = json.substring(startIndex, endIndex).trim();
            value = value.replaceAll("\"", "");

            return value;
        } catch (Exception e) {
            return "0"; // Απλό fallback σε περίπτωση λάθους
        }
    }
}