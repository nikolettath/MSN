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
    private static final String MASTER_IP = "localhost";
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
                System.out.println("3. Restore Game");
                System.out.println("4. Edit Game Risk");
                System.out.println("5. Edit Game Bet Limits");
                System.out.println("6. View Report (By Provider)");
                System.out.println("7. View Report (By Player)");
                System.out.println("0. Exit");
                System.out.print("Select: ");

                String choice = scanner.nextLine();
                if (choice.equals("0")) break;

                if (choice.equals("1")) addGameAction(out, in);
                else if (choice.equals("2")) {
                    System.out.print("Game name to remove: ");
                    out.writeObject("MANAGER_CMD|REMOVE|" + scanner.nextLine());
                    out.flush();
                    System.out.println("[Master]: " + in.readObject());
                }
                else if (choice.equals("3")) {
                    System.out.print("Game name to restore: ");
                    out.writeObject("MANAGER_CMD|RESTORE|" + scanner.nextLine());
                    out.flush();
                    System.out.println("[Master]: " + in.readObject());
                }
                else if (choice.equals("4")) {
                    System.out.print("Game name: "); String gName = scanner.nextLine();
                    System.out.print("New risk (low/medium/high): "); String risk = scanner.nextLine();
                    out.writeObject("MANAGER_CMD|EDIT_RISK|" + gName + "|" + risk);
                    out.flush();
                    System.out.println("[Master]: " + in.readObject());
                }
                else if (choice.equals("5")) {
                    System.out.print("Game name: "); String gName = scanner.nextLine();
                    System.out.print("New Min Bet: "); String min = scanner.nextLine();
                    System.out.print("New Max Bet: "); String max = scanner.nextLine();
                    out.writeObject("MANAGER_CMD|EDIT_LIMITS|" + gName + "|" + min + "|" + max);
                    out.flush();
                    System.out.println("[Master]: " + in.readObject());
                }
                else if (choice.equals("6")) {
                    out.writeObject("MANAGER_CMD|REPORT|BY_PROVIDER");
                    out.flush();
                    System.out.println("\n[REPORT BY PROVIDER]:\n" + in.readObject());
                }
                else if (choice.equals("7")) {
                    out.writeObject("MANAGER_CMD|REPORT|BY_PLAYER");
                    out.flush();
                    System.out.println("\n[REPORT BY PLAYER]:\n" + in.readObject());
                }
            }
        } catch (Exception e) { System.err.println("Error: " + e.getMessage()); }
    }

    private static void addGameAction(ObjectOutputStream out, ObjectInputStream in) {
        try {
            // 1. Σκανάρισμα του φακέλου για αρχεία .json
            File folder = new File("."); // Ο τρέχων φάκελος του project
            File[] jsonFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));

            if (jsonFiles == null || jsonFiles.length == 0) {
                System.out.println("[!] No .json files found in the project folder.");
                return;
            }

            // 2. Εμφάνιση λίστας αρχείων στον Manager
            System.out.println("\nAvailable JSON games found:");
            for (int i = 0; i < jsonFiles.length; i++) {
                System.out.println((i + 1) + ". " + jsonFiles[i].getName());
            }

            Scanner sc = new Scanner(System.in);
            System.out.print("Select file number to upload: ");
            int choice = Integer.parseInt(sc.nextLine());

            if (choice < 1 || choice > jsonFiles.length) {
                System.out.println("[!] Invalid selection.");
                return;
            }

            // 3. Ανάγνωση του επιλεγμένου αρχείου
            File selectedFile = jsonFiles[choice - 1];
            String content = new String(Files.readAllBytes(selectedFile.toPath()));

            // Μετατροπή σε αντικείμενο Game μέσω Gson
            JsonObject jsonObject = new Gson().fromJson(content, JsonObject.class);
            Game newGame = new Game(
                    jsonObject.get("gameName").getAsString(),
                    jsonObject.get("providerName").getAsString(),
                    jsonObject.get("stars").getAsInt(),
                    jsonObject.get("noOfVotes").getAsInt(),
                    jsonObject.get("gameLogo").getAsString(),
                    jsonObject.get("minBet").getAsDouble(),
                    jsonObject.get("maxBet").getAsDouble(),
                    jsonObject.get("riskLevel").getAsString(),
                    jsonObject.get("hashKey").getAsString());

            // 4. Αποστολή στον Master
            out.writeObject(newGame);
            out.flush();

            System.out.println("\n[Master Response]: " + in.readObject());

        } catch (Exception e) {
            System.out.println("[!] Error processing JSON: " + e.getMessage());
        }
    }
}
