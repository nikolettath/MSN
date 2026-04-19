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

        try
        {
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

            System.out.println("Game ready: " + newGame.getGameName());

            //send object w TCP
            try (Socket socket = new Socket(MASTER_IP, MASTER_PORT))
            {
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                //send object
                out.writeObject(newGame);
                out.flush();
                System.out.println("Game was sent. Waiting for response from Master...");

                //wait for approval or rejection from Master
                String response = (String) in.readObject();
                System.out.println("\n[Message from Master]: " + response);

            } catch (IOException e)
            {
                System.err.println("Connection error: " + e.getMessage());
                e.printStackTrace();
            }
        }catch (Exception e)
        {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // vriskei thn timh enos kleidiou mesa se JSON string
    private static String extractValue(String json, String key) {
        try {
            String searchFor = "\"" + key + "\":";
            int startIndex = json.indexOf(searchFor) + searchFor.length();
            int endIndex = json.indexOf(",", startIndex);

            //mporei na mhn exei komma alla agkyli an einai to teleutaio stoixeio
            if (endIndex == -1)
            {
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