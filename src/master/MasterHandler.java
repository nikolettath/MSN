package master;

import common.Game;
import request.FilterRequest;
import request.ReportRequest;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MasterHandler extends Thread {
    private Socket clientSocket;
    private List<WorkerInfo> workers;

    // Σταθερές επικοινωνίας για SRG και Reducer
    private static final String SRG_IP = "localhost";
    private static final int SRG_PORT = 9090;

    // Ορίζουμε πού τρέχει ο Reducer (χρησιμοποιείται στα requests προς τους Workers)
    private static final String REDUCER_IP = "localhost";
    private static final int REDUCER_PORT = 5000;

    public MasterHandler(Socket socket, List<WorkerInfo> workers) {
        this.clientSocket = socket;
        this.workers = workers;
    }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

            while (true) {
                Object request = in.readObject();

                // ---------------------------------------------------------
                // 1. ΛΗΨΗ ΑΠΟΤΕΛΕΣΜΑΤΩΝ ΑΠΟ REDUCER (Callback)
                // ---------------------------------------------------------
                // Αν ο Reducer συνδεθεί στον Master για να στείλει την τελική λίστα/αναφορά
                if (request instanceof List || request instanceof Map) {
                    System.out.println("[MASTER] Λήφθηκαν τελικά συγκεντρωτικά αποτελέσματα από τον Reducer.");
                    // Εδώ, σε αυτό το στάδιο (Παραδοτέο Α), ο Master απλά επιβεβαιώνει τη λήψη.
                    continue;
                }

                // ---------------------------------------------------------
                // 2. ΚΑΤΑΧΩΡΗΣΗ ΠΑΙΧΝΙΔΙΟΥ (Από Manager)
                // ---------------------------------------------------------
                if (request instanceof Game) {
                    handleNewGame((Game) request, out);
                }

                // ---------------------------------------------------------
                // 3. ΕΝΤΟΛΕΣ ΠΑΙΚΤΗ / MANAGER (Strings)
                // ---------------------------------------------------------
                else if (request instanceof String) {
                    String command = (String) request;

                    // --- ΛΕΙΤΟΥΡΓΙΑ: ΠΟΝΤΑΡΙΣΜΑ (BET) ---
                    if (command.startsWith("PLAYER_CMD|BET")) {
                        handleBetCommand(command, out);
                    }

                    // --- ΛΕΙΤΟΥΡΓΙΑ: ΑΝΑΖΗΤΗΣΗ (SEARCH - MapReduce) ---
                    else if (command.startsWith("PLAYER_CMD|SEARCH")) {
                        handleSearchCommand(command, out);
                    }

                    // --- ΛΕΙΤΟΥΡΓΙΑ: ΑΝΑΦΟΡΑ (REPORT - MapReduce) ---
                    else if (command.startsWith("MANAGER_CMD|REPORT")) {
                        handleReportCommand(command, out);
                    }
                }
            }
        } catch (EOFException e) {
            System.out.println("Ένας client αποσυνδέθηκε ομαλά.");
        } catch (Exception e) {
            System.err.println("Σφάλμα στον Master Handler: " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException e) { /* Ignore */ }
        }
    }

    /**
     * Υλοποίηση της 2ης Διόρθωσης: Έξυπνο Parse και Broadcast FilterRequest.
     */
    private void handleSearchCommand(String command, ObjectOutputStream out) throws IOException {
        String[] parts = command.split("\\|");
        String[] args = parts[1].split(" ");

        String category = null;
        String riskLevel = null;
        String provider = null;

        // Διατρέχουμε τα ορίσματα (αγνοώντας το 1ο που είναι η λέξη "SEARCH")
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];

            // Έλεγχος αν είναι Risk Level
            if (arg.equalsIgnoreCase("low") || arg.equalsIgnoreCase("medium") || arg.equalsIgnoreCase("high")) {
                riskLevel = arg.toLowerCase();
            }
            // Έλεγχος αν είναι Bet Category
            else if (arg.equals("$") || arg.equals("$$") || arg.equals("$$$")) {
                category = arg;
            }
            // Αν δεν είναι τίποτα από τα παραπάνω, το θεωρούμε όνομα Παρόχου (Provider)
            else {
                provider = arg;
            }
        }

        System.out.println("[MASTER] Έναρξη SEARCH. Φίλτρα -> Ρίσκο: " + riskLevel + ", Κατηγορία: " + category + ", Πάροχος: " + provider);

        // 1. Δημιουργία του αντικειμένου Request με όλα τα φίλτρα
        FilterRequest filterReq = new FilterRequest(category, provider, riskLevel, REDUCER_IP, REDUCER_PORT);

        // 2. Αποστολή σε ΟΛΟΥΣ τους Workers (Map Phase)
        int successCount = 0;
        for (WorkerInfo w : workers) {
            if (sendToWorkerGeneric(filterReq, w)) {
                successCount++;
            }
        }

        out.writeObject("Η αναζήτηση ξεκίνησε. " + successCount + " Workers ανταποκρίθηκαν. " +
                "Τα αποτελέσματα συγκεντρώνονται στον Reducer.");
        out.flush();
    }


    /**
     * Υλοποίηση Aggregation Query: Broadcast ReportRequest σε όλους τους Workers.
     */
    private void handleReportCommand(String command, ObjectOutputStream out) throws IOException {
        String[] parts = command.split("\\|");
        String type = parts[1]; // Πρέπει να είναι "BY_PROVIDER" ή "BY_PLAYER" [cite: 91-93]

        System.out.println("[MASTER] Έναρξη REPORT για: " + type);

        ReportRequest reportReq = new ReportRequest(type, REDUCER_IP, REDUCER_PORT);

        for (WorkerInfo w : workers) {
            sendToWorkerGeneric(reportReq, w);
        }

        out.writeObject("Το αίτημα για την οικονομική αναφορά (" + type + ") στάλθηκε για επεξεργασία.");
        out.flush();
    }

    /**
     * Χειρισμός Πονταρίσματος (Αποστολή σε ΕΝΑΝ συγκεκριμένο Worker βάσει Hash).
     */
    private void handleBetCommand(String command, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        String[] parts = command.split("\\|");
        String[] args = parts[1].split(" ");
        String gameName = args[1];
        String betAmount = args[2];

        // Hashing για εύρεση του σωστού Worker [cite: 64-65, 73]
        int workerIndex = Math.abs(gameName.hashCode()) % workers.size();
        WorkerInfo selectedWorker = workers.get(workerIndex);

        try (Socket workerSocket = new Socket(selectedWorker.getIp(), selectedWorker.getPort());
             ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
             ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream())) {

            workerOut.writeObject("BET|" + gameName + "|" + betAmount);
            workerOut.flush();

            String response = (String) workerIn.readObject();
            out.writeObject(response);
            out.flush();
        } catch (IOException e) {
            out.writeObject("ΣΦΑΛΜΑ: Ο Worker " + workerIndex + " δεν είναι διαθέσιμος.");
            out.flush();
        }
    }

    /**
     * Καταχώρηση νέου παιχνιδιού (SRG Register -> Worker Forward).
     */
    private void handleNewGame(Game game, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        System.out.println("Επεξεργασία νέου παιχνιδιού: " + game.getGameName());

        // 1. Εγγραφή στον SRG [cite: 63, 82]
        if (!registerWithSRG(game.getGameName(), game.getHashKey())) {
            out.writeObject("ΣΦΑΛΜΑ: Αποτυχία σύνδεσης με SRG Server.");
            out.flush();
            return;
        }

        // 2. Επιλογή Worker βάσει Hash
        int workerIndex = Math.abs(game.getGameName().hashCode()) % workers.size();
        WorkerInfo selectedWorker = workers.get(workerIndex);

        // 3. Προώθηση στον Worker
        if (sendToWorkerGeneric(game, selectedWorker)) {
            out.writeObject("ΕΠΙΤΥΧΙΑ: Το παιχνίδι '" + game.getGameName() + "' αποθηκεύτηκε στον Worker " + workerIndex);
        } else {
            out.writeObject("ΣΦΑΛΜΑ: Αποτυχία αποθήκευσης στον Worker " + workerIndex);
        }
        out.flush();
    }

    /**
     * Βοηθητική μέθοδος για αποστολή οποιουδήποτε Serializable αντικειμένου σε Worker.
     */
    private boolean sendToWorkerGeneric(Serializable obj, WorkerInfo worker) {
        try (Socket s = new Socket(worker.getIp(), worker.getPort());
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

            out.writeObject(obj);
            out.flush();

            Object response = in.readObject();
            return response != null; // Επιστρέφει true αν ο Worker απάντησε (π.χ. SUCCESS ή MAP_COMPLETED)
        } catch (Exception e) {
            System.err.println("[MASTER] Αποτυχία επικοινωνίας με Worker στην πόρτα " + worker.getPort());
            return false;
        }
    }

    private boolean registerWithSRG(String gameName, String hashKey) {
        try (Socket srgSocket = new Socket(SRG_IP, SRG_PORT);
             ObjectOutputStream srgOut = new ObjectOutputStream(srgSocket.getOutputStream());
             ObjectInputStream srgIn = new ObjectInputStream(srgSocket.getInputStream())) {

            srgOut.writeObject("REGISTER," + gameName + "," + hashKey);
            srgOut.flush();

            String response = (String) srgIn.readObject();
            return "OK".equals(response);
        } catch (Exception e) {
            return false;
        }
    }
}