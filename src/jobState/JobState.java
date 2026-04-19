package jobState;

import common.Game;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// xrhsimevei ws koino shmeio gia na mazevtoun ta apotelesmata apo pollous workers san reducer.
public class JobState {

    private final String requestId;
    private final int sumExpectedWorkers;
    private int ansReceived = 0;

    // domes gia na apothikeusoume ta sigkentrotika apotelesmata
    private final List<Game> finResults = new ArrayList<>();
    private final Map<String, Double> finReport = new HashMap<>();

    public JobState(String requestId, int sumExpectedWorkers) {
        this.requestId = requestId;
        this.sumExpectedWorkers = sumExpectedWorkers;
    }

    public String getRequestId() { return requestId; }


    // synchronized method gia na mhn pesoun mazi 2 workers kai xasoume dedomena
    public synchronized void addWorkerResult(List<Game> result) {

        finResults.addAll(result);      // prosthetoume ta games tou sygkekrimenou worker
        ansReceived++;                  // auxanoume ton counter twn apanthsewn
        System.out.println("[REDUCER] Downloaded data for ID " + requestId.substring(0,8) + "... (" + ansReceived + "/" + sumExpectedWorkers + ")");

        // wake ta threads pou mporei na einai blocked stin waitForListCompletion()
        notifyAll();
    }


    // kaleitai apo to thread pou thelei na parei to teliko apotelesma
    public synchronized List<Game> waitForListCompletion() throws InterruptedException {

        // oso den exoune apanthsei oloi oi workers to thread perimenei / blockarei
        while (ansReceived < sumExpectedWorkers) {
            wait();
        }
        System.out.println("[REDUCER] Completed list. Total games: " + finResults.size());

        // epistrefoume antigrafo gia na prostatepsoume thn eswterikh list
        return new ArrayList<>(finResults);
    }


    // kanoume merge ta dedomena enos Map (px. gia to aggregrate report)
    public synchronized void addWorkerReport(Map<String, Double> report) {

        for (Map.Entry<String, Double> entry : report.entrySet())
        {
            // an iparxei idi to key tou prosthetoume to kainourio value alliws to arxikopoioume
            finReport.put(entry.getKey(), finReport.getOrDefault(entry.getKey(), 0.0) + entry.getValue());
        }
        ansReceived++;
        System.out.println("[REDUCER] Downloaded report data for ID " + requestId.substring(0,8) + "... (" + ansReceived + "/" + sumExpectedWorkers + ")");

        notifyAll();
    }


    // perimenoume to teliko report
    public synchronized Map<String, Double> waitForReportCompletion() throws InterruptedException {

        while (ansReceived < sumExpectedWorkers) {
            wait();
        }
        System.out.println("[REDUCER] Final Report is finished.");
        return new HashMap<>(finReport);
    }
}