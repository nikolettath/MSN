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

    // ΝΕΟ: HashMap αντί για List για να γίνεται Deduplication (αφαίρεση διπλότυπων)
    private final Map<String, Game> uniqueGames = new HashMap<>();
    private final Map<String, Double> finReport = new HashMap<>();

    public JobState(String requestId, int sumExpectedWorkers) {
        this.requestId = requestId;
        this.sumExpectedWorkers = sumExpectedWorkers;
    }

    public String getRequestId() { return requestId; }

    public synchronized void addWorkerResult(List<Game> result) {
        for (Game g : result) {
            uniqueGames.put(g.getGameName(), g); // Αν υπάρχει ήδη, απλά το κάνει overwrite
        }
        ansReceived++;
        System.out.println("[REDUCER] Downloaded data for ID " + requestId.substring(0,8) + "... (" + ansReceived + "/" + sumExpectedWorkers + ")");
        notifyAll();
    }

    public synchronized List<Game> waitForListCompletion() throws InterruptedException {
        while (ansReceived < sumExpectedWorkers) {
            wait();
        }
        System.out.println("[REDUCER] Completed list. Unique games found: " + uniqueGames.size());
        return new ArrayList<>(uniqueGames.values());
    }

    public synchronized void addWorkerReport(Map<String, Double> report) {
        for (Map.Entry<String, Double> entry : report.entrySet()) {
            finReport.put(entry.getKey(), finReport.getOrDefault(entry.getKey(), 0.0) + entry.getValue());
        }
        ansReceived++;
        System.out.println("[REDUCER] Downloaded report data for ID " + requestId.substring(0,8) + "... (" + ansReceived + "/" + sumExpectedWorkers + ")");
        notifyAll();
    }

    public synchronized Map<String, Double> waitForReportCompletion() throws InterruptedException {
        while (ansReceived < sumExpectedWorkers) {
            wait();
        }
        System.out.println("[REDUCER] Final Report is finished.");
        return new HashMap<>(finReport);
    }
}