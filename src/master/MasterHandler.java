package master;

import common.Game;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class MasterHandler extends Thread {
    private Socket clientSocket;
    private List<WorkerInfo> workers;

    //SRG Server info (melos 3)
    private static final String SRG_IP = "localhost";
    private static final int SRG_PORT = 9090;

    public MasterHandler(Socket socket, List<WorkerInfo> workers) {
        this.clientSocket = socket;
        this.workers = workers;
    }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

            // ΒΑΖΟΥΜΕ WHILE(TRUE) ΓΙΑ ΝΑ ΚΡΑΤΗΣΟΥΜΕ ΤΗ ΣΥΝΔΕΣΗ ΑΝΟΙΧΤΗ
            while (true) {
                //read object from client
                Object request = in.readObject();

                //case: object from Manager App
                if (request instanceof Game) {
                    Game game = (Game) request;
                    System.out.println("Ελήφθη νέο παιχνίδι προς καταχώρηση: " + game.getGameName());

                    //update SRG server
                    boolean srgRegistered = registerWithSRG(game.getGameName(), game.getHashKey());

                    if (!srgRegistered) {
                        out.writeObject("ΣΦΑΛΜΑ: Ο SRG Server δεν είναι διαθέσιμος. Ακύρωση καταχώρησης.");
                        out.flush();
                        continue; // Χρησιμοποιούμε continue αντί για return για να μην κλείσει το socket!
                    }

                    //hashing & epilogh worker
                    int workerIndex = Math.abs(game.getGameName().hashCode()) % workers.size();
                    WorkerInfo selectedWorker = workers.get(workerIndex);

                    System.out.println("Δρομολόγηση του '" + game.getGameName() + "' στον Worker " + workerIndex);

                    //send game to worker
                    boolean workerSuccess = forwardToWorker(game, selectedWorker);

                    //update Manager App
                    if (workerSuccess) {
                        out.writeObject("ΕΠΙΤΥΧΙΑ: Το παιχνίδι καταχωρήθηκε σωστά στο σύστημα!");
                    } else {
                        out.writeObject("ΣΦΑΛΜΑ: Ο Worker (" + workerIndex + ") δεν ανταποκρίνεται.");
                    }
                    out.flush();

                }
                //case: commands from Player App
                else if (request instanceof String) {
                    String command = (String) request;

                    // --- ΛΟΓΙΚΗ ΠΟΝΤΑΡΙΣΜΑΤΟΣ (BET) ---
                    if (command.startsWith("PLAYER_CMD|BET")) {
                        String[] parts = command.split("\\|");
                        String[] args = parts[1].split(" ");

                        String gameName = args[1];
                        String betAmount = args[2];

                        //looking for worker with the same game (idio hashing)
                        int workerIndex = Math.abs(gameName.hashCode()) % workers.size();
                        WorkerInfo selectedWorker = workers.get(workerIndex);

                        // ΠΡΟΣΘΗΚΗ: catch block ΜΟΝΟ για τον Worker, ώστε να μην σπάει η κεντρική λούπα!
                        try (Socket workerSocket = new Socket(selectedWorker.getIp(), selectedWorker.getPort())) {
                            ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
                            ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream());

                            //sending bet to Worker
                            workerOut.writeObject("BET|" + gameName + "|" + betAmount);
                            workerOut.flush();

                            //read if won or lost and send result to Player
                            String workerResponse = (String) workerIn.readObject();
                            out.writeObject(workerResponse);
                            out.flush();

                        } catch (IOException e) {
                            System.err.println("[MASTER] Αποτυχία σύνδεσης με Worker " + workerIndex + " στην πόρτα " + selectedWorker.getPort());
                            out.writeObject("ΣΦΑΛΜΑ: Το παιχνίδι είναι προσωρινά μη διαθέσιμο (Ο Worker " + workerIndex + " είναι εκτός λειτουργίας).");
                            out.flush();
                        }
                    }
                    // --- ΝΕΟ: ΛΟΓΙΚΗ ΑΝΑΖΗΤΗΣΗΣ (SEARCH) ---
                    else if (command.startsWith("PLAYER_CMD|SEARCH")) {
                        String[] parts = command.split("\\|");
                        String[] args = parts[1].split(" ");

                        String riskLevel = args.length > 1 ? args[1] : "";

                        System.out.println("Ο παίκτης ζήτησε αναζήτηση με ρίσκο: " + riskLevel);

                        // TODO: Εδώ θα φτιάξεις το FilterRequest και θα το στείλεις
                        // σε ΟΛΟΥΣ τους Workers (γιατί δεν ξέρεις πού είναι αποθηκευμένα
                        // τα παιχνίδια αυτού του ρίσκου).

                        // Προσωρινή απάντηση για να δεις ότι δεν κλείνει η σύνδεση:
                        out.writeObject("Το αίτημα αναζήτησης ('" + riskLevel + "') παραλήφθηκε από τον Master.");
                        out.flush();
                    }
                }
            }

        } catch (EOFException e) {
            // Αυτό το exception είναι φυσιολογικό! Σημαίνει ότι ο Player πάτησε "EXIT"
            // και έκλεισε τη σύνδεση από τη μεριά του.
            System.out.println("Ένας client (Player/Manager) αποσυνδέθηκε ομαλά.");
        } catch (Exception e) {
            System.err.println("Σφάλμα στον Master Handler: " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException e) { /* Ignore */ }
        }
    }


    /**
     * Ανοίγουμε σύνδεση socket με τον SRG Server για την αποστολή της εντολής init
     */
    private boolean registerWithSRG(String gameName, String hashKey) throws ClassNotFoundException {
        try (Socket srgSocket = new Socket(SRG_IP, SRG_PORT);
             ObjectOutputStream srgOut = new ObjectOutputStream(srgSocket.getOutputStream());
             ObjectInputStream srgIn = new ObjectInputStream(srgSocket.getInputStream())) {

            // Μέσα στο registerWithSRG του MasterHandler:
            String command = "REGISTER," + gameName + "," + hashKey;
            srgOut.writeObject(command); // ΑΛΛΑΓΗ
            srgOut.flush();

            String response = (String) srgIn.readObject(); // ΑΛΛΑΓΗ
            return response.equals("OK");

        } catch (IOException e) {
            System.err.println("Αποτυχία σύνδεσης με SRG (" + SRG_IP + ":" + SRG_PORT + ")");
            return false;
        }
    }


    /**
     * Προωθούμε το παιχνίδι στον Worker που έχει επιλεγεί
     */
    private boolean forwardToWorker(Game game, WorkerInfo worker) {
        try (Socket workerSocket = new Socket(worker.getIp(), worker.getPort());
             ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
             ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream())) {

            workerOut.writeObject(game);
            workerOut.flush();

            String response = workerIn.readUTF();
            return response.equals("OK");

        } catch (IOException e) {
            System.err.println("Αποτυχία σύνδεσης με Worker στη θύρα " + worker.getPort());
            return false;
        }
    }
}