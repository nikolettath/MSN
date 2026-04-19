package monitor;

import common.Game;
import jobState.JobState;

import java.net.Socket;
import java.util.List;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class ResultMonitor extends Thread {

    private final JobState jobState;
    private final String masterIP;
    private final int masterPort;


    // updated constructor to receive data from master
    public ResultMonitor(JobState jobState, String masterIP, int masterPort) {
        this.jobState = jobState;
        this.masterIP = masterIP;
        this.masterPort = masterPort;
    }


    @Override
    public void run()
    {
        try
        {
            System.out.println("[MONITOR] Waiting for all the Worker's results.");

            // thread is in waiting until complete number of workers in jobState
            List<Game> finMergedList = jobState.waitForCompletion();

            System.out.println("[MONITOR] Results are ready.\n");
            System.out.println("Starting sending to Master.");

            // create socket for data return
            sendResults(finMergedList);

        } catch (InterruptedException e) {
            System.err.println("Monitor interrupted: " + e.getMessage());
        }
    }

    private void sendResults(List<Game> results) {  //sends results to master
        // open new TCP Socket to master
        try (Socket s = new Socket(masterIP, masterPort); ObjectOutputStream output = new ObjectOutputStream(s.getOutputStream()))
        {
            // send list as object (serializable)
            output.writeObject(results);
            output.flush();

            System.out.println("[MONITOR] Final list with " + results.size() + " games was successfully sent.");

        } catch (IOException e) {
            System.err.println("[MONITOR] Error while sending to Master: " + e.getMessage());
        }
    }
}
