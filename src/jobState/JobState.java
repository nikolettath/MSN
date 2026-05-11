package jobState;

import common.Game;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobState {
    private final String requestId;
    private final int sumExpectedWorkers;
    // arithmos apanthsewn apo workers
    private int ansReceived = 0;

    // map gia afairesh diplotypwn paixnidiwn (logw replicas)
    private final Map<String, Game> uniqueGames = new HashMap<>();
    // map gia ta synolika oikonomika stoixeia
    private final Map<String, Double> finReport = new HashMap<>();

    // flag pou deixnei an to job afora report h search
    private boolean isReportData = false;

    public JobState(String requestId, int sumExpectedWorkers) {
        this.requestId = requestId;
        this.sumExpectedWorkers = sumExpectedWorkers;
    }

    public String getRequestId() { return requestId; }

    // prosthkh apotelesmatwn anazhthshs
    public synchronized void addWorkerResult(List<Game> result) {
        for (Game g : result) {
            uniqueGames.put(g.getGameName(), g);
        }
        ansReceived++;
        notifyAll();
    }

    // ensomatwsh oikonomikwn statistikwn (sum)
    public synchronized void addWorkerReport(Map<String, Double> report) {
        isReportData = true;
        for (Map.Entry<String, Double> entry : report.entrySet()) {
            finReport.put(entry.getKey(), finReport.getOrDefault(entry.getKey(), 0.0) + entry.getValue());
        }
        ansReceived++;
        notifyAll();
    }

    // methodos anamonhs mexri na apanthsoun oloi oi workers
    public synchronized Object waitForCompletion() throws InterruptedException {
        while (ansReceived < sumExpectedWorkers) {
            wait();
        }

        // epistrofh tou swstou typou dedomenwn
        if (isReportData) {
            return new HashMap<>(finReport);
        } else {
            return new ArrayList<>(uniqueGames.values());
        }
    }
}