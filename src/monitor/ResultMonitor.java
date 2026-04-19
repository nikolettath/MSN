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
    private final boolean isReport;

    public ResultMonitor(JobState jobState, String masterIP, int masterPort, boolean isReport) {
        this.jobState = jobState;
        this.masterIP = masterIP;
        this.masterPort = masterPort;
        this.isReport = isReport;
    }

    @Override
    public void run() {
        try
        {
            if (isReport) {
                Map<String, Double> finalReport = jobState.waitForReportCompletion();
                sendResults(finalReport);
            } else {
                List<?> finMergedList = jobState.waitForListCompletion();
                sendResults(finMergedList);
            }

            // afairesh JobState apo th mnhmh tou ReducerMain
            ReducerMain.activeJobs.remove(jobState.getRequestId());

        } catch (InterruptedException e) {
            System.err.println("Monitor interrupted: " + e.getMessage());
        }
    }


    private void sendResults(Object results) {

        try (Socket s = new Socket(masterIP, masterPort);
             ObjectOutputStream output = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(s.getInputStream()))   // gia swsto handshake me ton Master prin termatisei h syndesh
        {
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