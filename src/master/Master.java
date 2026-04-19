package master;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Master {
    private static final int PORT = 4321;
    private List<WorkerInfo> workers;

    /**
     * Ο Constructor δέχεται πλέον τη λίστα με τους Workers
     * που δημιουργήθηκε δυναμικά στη main.
     */
    public Master(List<WorkerInfo> workers) {
        this.workers = workers;
    }

    public void listen() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("==============================================");
            System.out.println("Master Node ξεκίνησε στη θύρα: " + PORT);
            System.out.println("Συνδεδεμένοι Workers: " + workers.size());
            for (int i = 0; i < workers.size(); i++) {
                System.out.println("   Worker " + i + ": " + workers.get(i).getIp() + ":" + workers.get(i).getPort());
            }
            System.out.println("==============================================");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[MASTER] Νέα σύνδεση από client: " + socket.getInetAddress());

                // Δημιουργία νέου thread για κάθε client
                new MasterHandler(socket, workers).start();
            }
        } catch (IOException e) {
            System.err.println("Σφάλμα κατά την εκκίνηση του ServerSocket στον Master: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Έλεγχος αν δόθηκε ο αριθμός των workers ως όρισμα
        if (args.length < 1) {
            System.out.println("Χρήση: java master.Master <number_of_workers>");
            System.out.println("Παράδειγμα: java master.Master 3");

            // Προαιρετικά: Αν δεν δοθούν ορίσματα, μπορούμε να ορίσουμε μια default τιμή
            // για να διευκολύνουμε τις δοκιμές, αλλά εμφανίζουμε προειδοποίηση.
            System.out.println("\n[!] Δεν δόθηκαν ορίσματα. Εκκίνηση με default 3 workers.");
            args = new String[]{"3"};
        }

        int numWorkers = Integer.parseInt(args[0]);
        List<WorkerInfo> dynamicWorkers = new ArrayList<>();

        // Δυναμική δημιουργία των WorkerInfo.
        // Εδώ υποθέτουμε ότι οι Workers τρέχουν στο localhost ξεκινώντας από την πόρτα 8081.
        // Σε μια πραγματική κατανεμημένη εφαρμογή, θα μπορούσαμε να διαβάζουμε
        // ένα config αρχείο με IP και Ports.
        for (int i = 0; i < numWorkers; i++) {
            int workerPort = 8081 + i;
            dynamicWorkers.add(new WorkerInfo("127.0.0.1", workerPort));
        }

        // Εκκίνηση του Master με τη δυναμική λίστα
        Master master = new Master(dynamicWorkers);
        master.listen();
    }
}