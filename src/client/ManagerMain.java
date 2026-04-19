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
            //reading file
            String content = new String(Files.readAllBytes(Paths.get(jsonPath)));

            //parsing json
            String gameName = extractValue(content, "gameName");
            String providerName = extractValue(content, "providerName");
            int stars = Integer.parseInt(extractValue(content, "stars"));
            int noOfVotes = Integer.parseInt(extractValue(content, "noOfVotes"));
            String gameLogo = extractValue(content, "gameLogo");
            double minBet = Double.parseDouble(extractValue(content, "minBet"));
            double maxBet = Double.parseDouble(extractValue(content, "maxBet"));
            String riskLevel = extractValue(content, "riskLevel");
            String hashKey = extractValue(content, "hashKey");

            //game object
            Game newGame = new Game(gameName, providerName, stars, noOfVotes,
                    gameLogo, minBet, maxBet, riskLevel, hashKey);

            System.out.println("Παιχνίδι έτοιμο: " + newGame.getGameName());

            //send object w TCP
            try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) { // <-- Προσθήκη InputStream

                out.flush();
                out.writeObject(newGame);
                out.flush();
                System.out.println("Το παιχνίδι στάλθηκε. Αναμονή απάντησης από Master...");

                //wait for master to confirm or reject
                String response = in.readUTF();
                System.out.println("\n[ΜΗΝΥΜΑ ΑΠΟ MASTER]: " + response);
            }

        } catch (Exception e) {
            System.err.println("Σφάλμα: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Μέθοδος για να βρεθεί η τιμή ενός κλειδιού μέσα σε ένα JSON string.
     */
    private static String extractValue(String json, String key) {
        try {
            String searchFor = "\"" + key + "\":";
            int startIndex = json.indexOf(searchFor) + searchFor.length();
            int endIndex = json.indexOf(",", startIndex);

            //mporei na mhn exei komma alla agkyli an einai to teleutaio stoixeio
            if (endIndex == -1) {
                endIndex = json.indexOf("}", startIndex);
            }

            //cleaning apo kena kai dipla eisagwgika
            String value = json.substring(startIndex, endIndex).trim();
            value = value.replaceAll("\"", "");

            return value;
        } catch (Exception e) {
            return "0";
        }
    }
}