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

    public JobState(String requestId, int sumExpectedWorkers) {
        this.requestId = requestId;
        this.sumExpectedWorkers = sumExpectedWorkers;
    }

    public String getRequestId() { return requestId; }

    public synchronized void addWorkerResult(List<Game> result) {
        for (Game g : result) uniqueGames.put(g.getGameName(), g);
        ansReceived++;
        notifyAll();
    }

    public synchronized List<Game> waitForListCompletion() throws InterruptedException {
        while (ansReceived < sumExpectedWorkers) wait();
        return new ArrayList<>(uniqueGames.values());
    }

    public synchronized void addWorkerReport(Map<String, Double> report) {
        for (Map.Entry<String, Double> entry : report.entrySet()) {
            finReport.put(entry.getKey(), finReport.getOrDefault(entry.getKey(), 0.0) + entry.getValue());
        }
        ansReceived++;
        notifyAll();
    }

    public synchronized Map<String, Double> waitForReportCompletion() throws InterruptedException {
        while (ansReceived < sumExpectedWorkers) wait();
        return new HashMap<>(finReport);
    }
}