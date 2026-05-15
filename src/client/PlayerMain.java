package client;

import common.Game;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

public class PlayerMain {
    private static final String MASTER_IP = "172.20.10.2";
    private static final int MASTER_PORT = 4321;
    private static String playerName;
    // topiko portofoli paikth
    public static double localBalance = 0.0;

    public static void main(String[] args) {
        System.out.println("----- Casino Player Console -----");
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username: ");
        playerName = scanner.nextLine();

        // syndesh me Master mesw TCP socket
        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // thread gia asygxronh lhpsh apanthsewn
            ServerListener listener = new ServerListener(in);
            listener.start();

            // loop tou menou
            while (true) {
                Thread.sleep(500);
                System.out.println("\n--- Welcome " + playerName + " (Balance: " + localBalance + " FUN) ---");
                System.out.println("1. Add Balance");
                System.out.println("2. Search Games");
                System.out.println("3. Bet on Game");
                System.out.println("4. Rate a Game");
                System.out.println("0. Exit");
                System.out.print("> ");

                String choice = scanner.nextLine();
                if (choice.equals("0")) break;

                if (choice.equals("1")) {
                    System.out.print("Amount to deposit: ");
                    double amount = Double.parseDouble(scanner.nextLine());
                    localBalance += amount;
                    System.out.println("Added " + amount + " FUN locally.");
                }
                else if (choice.equals("2")) {
                    // eisagwgh filtrwn anazhthshs
                    System.out.print("Risk (low/medium/high/ANY): ");
                    String risk = scanner.nextLine().trim();
                    if(risk.isEmpty()) risk = "ANY";

                    System.out.print("Category ($ / $$ / $$$ / ANY): ");
                    String cat = scanner.nextLine().trim();
                    if(cat.isEmpty()) cat = "ANY";

                    System.out.print("Provider (e.g. CasinoTech / ANY): ");
                    String prov = scanner.nextLine().trim();
                    if(prov.isEmpty()) prov = "ANY";

                    System.out.print("Min Stars (1-5, or 0 for ANY): ");
                    String strStars = scanner.nextLine();
                    int stars = strStars.isEmpty() ? 0 : Integer.parseInt(strStars);

                    // apostolh search request
                    out.writeObject("PLAYER_CMD|SEARCH|" + risk + "|" + cat + "|" + prov + "|" + stars);
                    out.flush();
                }
                else if (choice.equals("3")) {
                    System.out.print("Game name: ");
                    String gName = scanner.nextLine();
                    System.out.print("Bet amount: ");
                    double bet = Double.parseDouble(scanner.nextLine());

                    // elegxos topiku ypuloipou
                    if (bet > localBalance) {
                        System.out.println("ERROR: Insufficient local balance!");
                    } else {
                        localBalance -= bet;
                        // apostolh play request
                        out.writeObject("PLAYER_CMD|BET|" + gName + "|" + bet + "|" + playerName);
                        out.flush();
                    }
                }
                else if (choice.equals("4")) {
                    System.out.print("Game name: ");
                    String gName = scanner.nextLine();
                    System.out.print("Stars (1-5): ");
                    String stars = scanner.nextLine();
                    // apostolh rate request
                    out.writeObject("PLAYER_CMD|RATE|" + gName + "|" + stars);
                    out.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    // thread pou akouei tis apanthseis
    private static class ServerListener extends Thread {
        private final ObjectInputStream in;
        public ServerListener(ObjectInputStream in) { this.in = in; }

        @Override
        public void run() {
            try {
                while (true) {
                    Object response = in.readObject();

                    // emfanizh apotelesmatwn search
                    if (response instanceof List<?>) {
                        @SuppressWarnings("unchecked")
                        List<Game> games = (List<Game>) response;
                        System.out.println("\n\n[Search Results]:");
                        if (games.isEmpty()) System.out.println("No games found.");
                        else {
                            for (Game g : games) {
                                System.out.println(" • " + g.getGameName() + " | Provider: " + g.getProviderName() +
                                        " | Risk: " + g.getRiskLevel() + " | Category: " + g.getBetCategory() + " | Stars: " + g.getStars());
                            }
                        }
                    } else if (response instanceof String) {
                        String msg = (String) response;

                        // diaxeirish apotelesmatos bet
                        if (msg.startsWith("PAYOUT_RESULT|")) {
                            String[] parts = msg.split("\\|");
                            double wonAmount = Double.parseDouble(parts[1]);
                            localBalance += wonAmount;
                            System.out.println("\n\n[Bet Result]: " + parts[2]);
                        }
                        // diaxeirish epistrofhs xrhmatwn
                        else if (msg.startsWith("REFUND|")) {
                            String[] parts = msg.split("\\|");
                            localBalance += Double.parseDouble(parts[1]);
                            System.out.println("\n\n[System Error]: " + parts[2] + " (Bet Refunded)");
                        } else {
                            // genika mhnymata
                            System.out.println("\n\n[Message]: " + msg);
                        }
                    }
                    System.out.print("\n> ");
                }
            } catch (Exception e) {
                System.out.println("\n[!] Connection closed.");
            }
        }
    }
}