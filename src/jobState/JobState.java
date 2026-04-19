package jobState;

import common.Game;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class JobState {

    private final int sumExpectedWorkers;
    private int ansReceived = 0;    // synolikes apanthseis

    private final List<Game> finResults = new ArrayList<>();                 // filters (List)
    private final Map<String, Double> finReport = new HashMap<>();           // reports (Map)


    public JobState(int sumExpectedWorkers) {
        this.sumExpectedWorkers = sumExpectedWorkers;
    }

    // filtering paixnidiwn (lists)
    // kaleitai apo to ReducerThread otan lambanei List<Game>
    public synchronized void addWorkerResult(List<Game> result) {
        finResults.addAll(result);      // sygxwneush lists
        ansReceived++;
        System.out.println("[REDUCER] Downloaded data from Worker. (" + ansReceived + "/" + sumExpectedWorkers + ")");

        notifyAll();    // xypnaei to monitor thread
    }

    // kaleitai apo to monitor thread pou perimenei list
    public synchronized List<Game> waitForListCompletion() throws InterruptedException {
        while (ansReceived < sumExpectedWorkers) {
            wait();
        }
        System.out.println("[REDUCER] Completed list. Total games: " + finResults.size());
        return new ArrayList<>(finResults);
    }


    // oikonomika reports (maps)
    // kaleitai apo to ReducerThread otan lambanei Map<String, Double>
    public synchronized void addWorkerReport(Map<String, Double> report) {
        // diatrexei to HashMap tou Worker kai prosthetei tis times
        for (Map.Entry<String, Double> entry : report.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue();
            finReport.put(key, finReport.getOrDefault(key, 0.0) + value);
        }

        ansReceived++;
        System.out.println("[REDUCER] Downloaded partial report. (" + ansReceived + "/" + sumExpectedWorkers + ")");

        notifyAll();    // xypnaei monitor thread
    }

    // kaleitai apo to monitor thread pou perimenei map
    public synchronized Map<String, Double> waitForReportCompletion() throws InterruptedException {
        while (ansReceived < sumExpectedWorkers) {
            wait();
        }
        System.out.println("[REDUCER] Final Report is finished.");
        return new HashMap<>(finReport);
    }
}