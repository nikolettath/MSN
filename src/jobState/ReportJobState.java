package jobState;

import java.util.Map;
import java.util.HashMap;

public class ReportJobState {

    private final int sumExpectedWorkers;
    private int answReceived = 0;   //answers received

    private final Map<String, Double> finReport = new HashMap<>();      //final report with summed up results

    public ReportJobState(int sumExpectedWorkers) { this.sumExpectedWorkers = sumExpectedWorkers;}

    // reduce is called from ReducerThread when a Worker sends its own HashMap
    public synchronized void addWorkerReport(Map<String, Double> report) {

        // traversing worker's HashMap and adding up values to final HashMap
        for (Map.Entry<String, Double> entry : report.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue();

            // finReport[key] = finReport[key] + value
            finReport.put(key, finReport.getOrDefault(key, 0.0) + value);
        }

        answReceived++;
        System.out.println("[REDUCER] Downloaded partial report. (" + answReceived + "/" + sumExpectedWorkers + ")");

        notifyAll(); // waking monitor thread
    }


    // called by monitor thread
    public synchronized Map<String, Double> waitForCompletion() throws InterruptedException {

        while (answReceived < sumExpectedWorkers) {
            wait();
        }
        System.out.println("[REDUCER] Final Report is finished.");
        return new HashMap<>(finReport);    // returns copy
    }
}
