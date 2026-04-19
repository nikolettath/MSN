package jobState;

import common.Game;

import java.util.List;
import java.util.ArrayList;

public class JobState {

    private final int sumExpectedWorkers;
    private int ansReceived = 0;    //answers received

    private final List<Game> finResults = new ArrayList<>();    //final merged results list


    public JobState(int sumExpectedWorkers) { this.sumExpectedWorkers = sumExpectedWorkers; }


    // called by ReducerThread everytime a worker sends its list
    public synchronized void addWorkerResult(List<Game> result) {

        finResults.addAll(result);  // merging lists (reduce)
        ansReceived++;
        System.out.println("[REDUCER] Downloaded data from Worker. (" + ansReceived + "/" + sumExpectedWorkers + ")");

        notifyAll();    // waking whatever thread is waiting ( wait() )
    }


    // called by thread that wants to get the final result
    public synchronized List<Game> waitForCompletion() throws InterruptedException {

        while (ansReceived < sumExpectedWorkers) {      // avoiding spurious wakeup
            wait();
        }

        //  ansReceived == sumExpectedWorkers
        System.out.println("[REDUCER] Completed list. Total games: " + finResults.size());
        return new ArrayList<>(finResults);
    }
}
