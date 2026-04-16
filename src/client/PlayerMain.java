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

            // 1. Αρχικοποίηση Streams (SOS: Πάντα πρώτα το Output, μετά flush, μετά το Input!)
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            // 2. Εκκίνηση του Ασύγχρονου Listener (Το Thread μας)
            // Αυτό το νήμα θα "ακούει" μόνιμα τον Master χωρίς να μπλοκάρει το πληκτρολόγιο!
            ServerListener listener = new ServerListener(in);
            listener.start();

            // 3. Βρόχος διαβάσματος από το πληκτρολόγιο (Main Thread)
            Scanner scanner = new Scanner(System.in);
            printMenu();

            while (true) {
                String userInput = scanner.nextLine();

                if (userInput.equalsIgnoreCase("EXIT")) {
                    System.out.println("Αποσύνδεση...");
                    break;
                }

                // Στέλνουμε την εντολή στον Master (χρησιμοποιούμε ένα απλό String protocol)
                // π.χ. "SEARCH|high" ή "BET|SuperSlots|50"
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

    /**
     * Εσωτερική κλάση (Thread) που περιμένει συνεχώς μηνύματα/αποτελέσματα
     * από τον Master Node, ασύγχρονα.
     */
    private static class ServerListener extends Thread {
        private final ObjectInputStream in;

        public ServerListener(ObjectInputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    // Το readUTF() μπλοκάρει μέχρι να έρθει μήνυμα,
                    // αλλά επειδή είμαστε σε δικό μας Thread, το πληκτρολόγιο συνεχίζει να δουλεύει!
                    String response = in.readUTF();

                    System.out.println("\n\n[Μήνυμα από Σύστημα]: \n" + response);
                    System.out.print("> "); // Επαναφορά του κέρσορα για να γράψει ξανά ο χρήστης
                }
            } catch (IOException e) {
                System.out.println("\n[!] Η σύνδεση με τον Master τερματίστηκε.");
            }
        }
    }
}