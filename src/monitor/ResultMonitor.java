package monitor;

import jobState.JobState;
import request.FinalResponse;
import reducer.ReducerMain;

import java.io.ObjectInputStream;
import java.net.Socket;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.List;

public class ResultMonitor extends Thread {
    private final JobState jobState;
    private final String masterIP;
    private final int masterPort;

    // ΑΛΛΑΓΗ 1: Αφαιρέθηκε το boolean isReport από τον constructor
    public ResultMonitor(JobState jobState, String masterIP, int masterPort) {
        this.jobState = jobState;
        this.masterIP = masterIP;
        this.masterPort = masterPort;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        try {
            // ΑΛΛΑΓΗ 2: Καλούμε μια κοινή μέθοδο που περιμένει και επιστρέφει το τελικό Object
            Object finalResults = jobState.waitForCompletion();

            // Έξυπνος έλεγχος: Αν είναι Map, το κάνουμε ένα ωραίο String (Report)
            if (finalResults instanceof Map<?, ?>) {
                Map<String, Double> map = (Map<String, Double>) finalResults;
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Double> entry : map.entrySet()) {
                    sb.append(" • ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" FUN\n");
                }
                if (sb.length() == 0) sb.append("No financial data found.");

                sendResults(sb.toString());
            }
            // Έξυπνος έλεγχος: Αν είναι List, το στέλνουμε όπως είναι (Search)
            else {
                sendResults(finalResults);
            }

            // afairesh JobState apo th mnhmh tou ReducerMain me asfaleia
            synchronized (ReducerMain.activeJobs) {
                ReducerMain.activeJobs.remove(jobState.getRequestId());
            }

        } catch (InterruptedException e) {
            System.err.println("Monitor interrupted: " + e.getMessage());
        }
    }

    private void sendResults(Object results) {
        try (Socket s = new Socket(masterIP, masterPort);
             ObjectOutputStream output = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(s.getInputStream())) {

            // stelnei to ID kai ta apotelesmata
            String id = jobState.getRequestId();
            output.writeObject(new FinalResponse(id, results));
            output.flush();

            System.out.println("[MONITOR] Final results successfully sent to Master.");

        } catch (IOException e) {
            System.err.println("[MONITOR] Error while sending to Master: " + e.getMessage());
        }
    }
}