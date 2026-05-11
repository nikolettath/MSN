package jobState;

import common.Game;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobState {
    private final String requestId;
    private final int sumExpectedWorkers;
    private int ansReceived = 0;

    private final Map<String, Game> uniqueGames = new HashMap<>();
    private final Map<String, Double> finReport = new HashMap<>();

    // ΝΕΑ ΜΕΤΑΒΛΗΤΗ: Θυμάται αν τα δεδομένα που ήρθαν είναι Report ή Search
    private boolean isReportData = false;

    public JobState(String requestId, int sumExpectedWorkers) {
        this.requestId = requestId;
        this.sumExpectedWorkers = sumExpectedWorkers;
    }

    public String getRequestId() { return requestId; }

    public synchronized void addWorkerResult(List<Game> result) {
        // Η δική σου έξυπνη λογική για αφαίρεση διπλότυπων μέσω Map
        for (Game g : result) {
            uniqueGames.put(g.getGameName(), g);
        }
        ansReceived++;
        notifyAll();
    }

    public synchronized void addWorkerReport(Map<String, Double> report) {
        isReportData = true; // Σημειώνουμε ότι πρόκειται για Report
        for (Map.Entry<String, Double> entry : report.entrySet()) {
            finReport.put(entry.getKey(), finReport.getOrDefault(entry.getKey(), 0.0) + entry.getValue());
        }
        ansReceived++;
        notifyAll();
    }

    // Η ΝΕΑ ΚΟΙΝΗ ΜΕΘΟΔΟΣ ΑΝΑΜΟΝΗΣ ΠΟΥ ΚΑΛΕΙ ΤΟ MONITOR!
    public synchronized Object waitForCompletion() throws InterruptedException {
        while (ansReceived < sumExpectedWorkers) {
            wait();
        }

        // Επιστρέφει αυτόματα τον σωστό τύπο δεδομένων
        if (isReportData) {
            return new HashMap<>(finReport);
        } else {
            return new ArrayList<>(uniqueGames.values());
        }
    }
}