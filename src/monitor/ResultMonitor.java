package monitor;

import common.Game;
import jobState.JobState;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class ResultMonitor extends Thread {

    private final JobState jobState;
    private final String masterIP;
    private final int masterPort;

    // ΝΕΟ: Διακόπτης που λέει στο Monitor τι είδους αποτέλεσμα περιμένει
    private final boolean isReport;

    // Ενημερωμένος constructor
    public ResultMonitor(JobState jobState, String masterIP, int masterPort, boolean isReport) {
        this.jobState = jobState;
        this.masterIP = masterIP;
        this.masterPort = masterPort;
        this.isReport = isReport;
    }

    @Override
    public void run() {
        try {
            System.out.println("[MONITOR] Waiting for all the Worker's results. (isReport: " + isReport + ")");

            if (isReport) {
                // Περίπτωση 1: Ο Manager ζήτησε Οικονομική Αναφορά (Report)
                Map<String, Double> finalReport = jobState.waitForReportCompletion();

                System.out.println("[MONITOR] Report results are ready.\nStarting sending to Master.");
                sendResults(finalReport); // Στέλνουμε το Map

            } else {
                // Περίπτωση 2: Ο Παίκτης ζήτησε Φιλτράρισμα Παιχνιδιών (List)
                List<Game> finMergedList = jobState.waitForListCompletion();

                System.out.println("[MONITOR] List results are ready.\nStarting sending to Master.");
                sendResults(finMergedList); // Στέλνουμε τη Λίστα
            }

        } catch (InterruptedException e) {
            System.err.println("Monitor interrupted: " + e.getMessage());
        }
    }

    /**
     * Η μέθοδος πλέον δέχεται γενικά ένα Object (μπορεί να είναι List ή Map),
     * καθώς και τα δύο είναι Serializable.
     */
    private void sendResults(Object results) {
        // open new TCP Socket to master
        try (Socket s = new Socket(masterIP, masterPort);
             ObjectOutputStream output = new ObjectOutputStream(s.getOutputStream()))
        {
            // send object (serializable)
            output.writeObject(results);
            output.flush();

            // Ένα δυναμικό μήνυμα για να ξέρουμε τι στάλθηκε
            if (results instanceof List) {
                System.out.println("[MONITOR] Final list with " + ((List<?>) results).size() + " games was successfully sent.");
            } else if (results instanceof Map) {
                System.out.println("[MONITOR] Final report with " + ((Map<?, ?>) results).size() + " entries was successfully sent.");
            }

        } catch (IOException e) {
            System.err.println("[MONITOR] Error while sending to Master: " + e.getMessage());
        }
    }
}