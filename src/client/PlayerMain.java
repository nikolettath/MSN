package client;

import common.Game;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;


public class PlayerMain {

    private static final String MASTER_IP = "localhost";
    private static final int MASTER_PORT = 4321;


    public static void main(String[] args) {
        System.out.println("-----Casino Console App-----");

        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT))
        {
            // arxikopoihsh streams
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // starting asygxronou listener pou akouei ton master
            ServerListener listener = new ServerListener(in);
            listener.start();

            // reading apo to keyboard
            Scanner scanner = new Scanner(System.in);
            printMenu();

            while (true)
            {
                String userInput = scanner.nextLine();

                if (userInput.equalsIgnoreCase("EXIT"))
                {
                    System.out.println("Disconnecting...");
                    break;
                }

                // stelnei ston master
                out.writeObject("PLAYER_CMD|" + userInput);
                out.flush();
            }

        } catch (IOException e)
        {
            System.err.println("Connection error with Master: " + e.getMessage());
        }
    }


    private static void printMenu() {
        System.out.println("\nAvailable Commands:");
        System.out.println("- To search: SEARCH <risk_level> (e.g., SEARCH high)");
        System.out.println("- To bet: BET <game_name> <amount> (e.g., BET SuperSlots 20)");
        System.out.println("- Exit: EXIT");
        System.out.print("\n> ");
    }


    private static class ServerListener extends Thread {
        private final ObjectInputStream in;

        public ServerListener(ObjectInputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            try
            {
                while (true)
                {
                    // diabazoume ena genikoObject
                    Object response = in.readObject();

                    // an to apotelesma einai list (px. apo th SEARCH)
                    if (response instanceof List<?>)
                    {
                        @SuppressWarnings("unchecked")
                        List<Game> games = (List<Game>) response;

                        System.out.println("\n\n[Search Results]:");
                        if (games.isEmpty()) {
                            System.out.println("No games found with the given criteria.");
                        } else {
                            for (Game g : games) {
                                System.out.println(" • " + g.getGameName() + " (Provider: " + g.getProviderName() +
                                        ", Category: " + g.getBetCategory() + ", Risk: " + g.getRiskLevel() + ")");
                            }
                        }
                    }
                    // an to apotelesma einai aplo String (px. apotelesma BET h mhnuma lathous)
                    else if (response instanceof String)
                    {
                        System.out.println("\n\n[System message]: \n" + response);
                    }
                    // an einai otidhpote allo
                    else
                    {
                        System.out.println("\n\n[System Message]: Unknown data format received.");
                    }

                    System.out.print("\n> ");   // epanafora kersora
                }
            } catch (IOException e) {
                System.out.println("\n[!] Connection with Master terminated.");
            } catch (ClassNotFoundException e) {
                System.err.println("Error: Class not found during reading.");
            }
        }
    }
}