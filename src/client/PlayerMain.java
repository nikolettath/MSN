package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class PlayerMain {

    private static final String MASTER_IP = "localhost";
    private static final int MASTER_PORT = 4321;

    public static void main(String[] args) {
        System.out.println("=== Καλωσήρθατε στο Casino Console App ===");

        try (Socket socket = new Socket(MASTER_IP, MASTER_PORT)) {

            //arxikopoihsh streams
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            //starting asygxronou listener pou akouei ton master
            ServerListener listener = new ServerListener(in);
            listener.start();

            //reading from keyboard
            Scanner scanner = new Scanner(System.in);
            printMenu();

            while (true) {
                String userInput = scanner.nextLine();

                if (userInput.equalsIgnoreCase("EXIT")) {
                    System.out.println("Αποσύνδεση...");
                    break;
                }

                //sending to master
                out.writeUTF("PLAYER_CMD|" + userInput);
                out.flush();
            }

        } catch (IOException e) {
            System.err.println("Σφάλμα σύνδεσης με τον Master: " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("\nΔιαθέσιμες Εντολές:");
        System.out.println("- Για αναζήτηση: SEARCH <risk_level> (π.χ. SEARCH high)");
        System.out.println("- Για ποντάρισμα: BET <game_name> <amount> (π.χ. BET SuperSlots 20)");
        System.out.println("- Έξοδος: EXIT");
        System.out.print("\n> ");
    }

    private static class ServerListener extends Thread {
        private final ObjectInputStream in;

        public ServerListener(ObjectInputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    //to keyboard synexizei na doulevei kai as kanei block toreadUTF, giati ine se diko mas thread
                    String response = in.readUTF();

                    System.out.println("\n\n[Μήνυμα από Σύστημα]: \n" + response);
                    System.out.print("> "); //epanafora kersora
                }
            } catch (IOException e) {
                System.out.println("\n[!] Η σύνδεση με τον Master τερματίστηκε.");
            }
        }
    }
}