package client;

import common.Game;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class ManagerMain {
    private static final String MASTER_IP = "172.20.10.2";
    private static final int MASTER_PORT = 4321;

    public static void main(String[] args) {
        System.out.println("----- Manager Console -----");

        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.flush();
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("\n--- Options ---");
                System.out.println("1. Add Game (from game2.json)");
                System.out.println("2. Remove Game");
                System.out.println("3. Edit Game Risk");
                System.out.println("4. View Report (By Provider)");
                System.out.println("5. View Report (By Player)");
                System.out.println("0. Exit");
                System.out.print("Select: ");

                String choice = scanner.nextLine();

                if (choice.equals("0")) break;

                if (choice.equals("1")) {
                    addGameAction(out, in);
                }
                else if (choice.equals("2")) {
                    System.out.print("Enter game name to remove: ");
                    String gName = scanner.nextLine();
                    out.writeObject("MANAGER_CMD|REMOVE|" + gName);
                    out.flush();
                    System.out.println("[Master]: " + in.readObject());
                }
                else if (choice.equals("3")) {
                    System.out.print("Enter game name: ");
                    String gName = scanner.nextLine();
                    System.out.print("Enter new risk (low/medium/high): ");
                    String risk = scanner.nextLine();
                    out.writeObject("MANAGER_CMD|EDIT_RISK|" + gName + "|" + risk);
                    out.flush();
                    System.out.println("[Master]: " + in.readObject());
                }
                else if (choice.equals("4")) {
                    out.writeObject("MANAGER_CMD|REPORT|BY_PROVIDER");
                    out.flush();
                    System.out.println("Fetching report... please wait.");
                    // O Master epistrefei Map<String, Double> meso tou Reducer
                    Object result = in.readObject();
                    System.out.println("\n[REPORT BY PROVIDER]:\n" + result);
                }
                else if (choice.equals("5")) {
                    out.writeObject("MANAGER_CMD|REPORT|BY_PLAYER");
                    out.flush();
                    System.out.println("Fetching report... please wait.");
                    Object result = in.readObject();
                    System.out.println("\n[REPORT BY PLAYER]:\n" + result);
                }
            }

        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    private static void addGameAction(ObjectOutputStream out, ObjectInputStream in) {
        try {
            String content = new String(Files.readAllBytes(Paths.get("game2.json")));
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(content, JsonObject.class);

            Game newGame = new Game(
                    jsonObject.get("gameName").getAsString(),
                    jsonObject.get("providerName").getAsString(),
                    jsonObject.get("stars").getAsInt(),
                    jsonObject.get("noOfVotes").getAsInt(),
                    jsonObject.get("gameLogo").getAsString(),
                    jsonObject.get("minBet").getAsDouble(),
                    jsonObject.get("maxBet").getAsDouble(),
                    jsonObject.get("riskLevel").getAsString(),
                    jsonObject.get("hashKey").getAsString()
            );

            out.writeObject(newGame);
            out.flush();
            System.out.println("\n[Master]: " + in.readObject());

        } catch (Exception e) {
            System.out.println("Failed to load JSON: " + e.getMessage());
        }
    }
}