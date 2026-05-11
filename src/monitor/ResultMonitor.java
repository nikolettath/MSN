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

    // arxikopoihsh tou monitor me ta aparaithta stoixeia
    public ResultMonitor(JobState jobState, String masterIP, int masterPort) {
        this.jobState = jobState;
        this.masterIP = masterIP;
        this.masterPort = masterPort;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        try {
            // anamonh gia thn oloklhrwsh kai lhpsh twn telikwn dedomenwn
            Object finalResults = jobState.waitForCompletion();

            // an prokeitai gia oikonomiko report (Map), ftiakxnei to teliko keimeno
            if (finalResults instanceof Map<?, ?>) {
                Map<String, Double> map = (Map<String, Double>) finalResults;
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Double> entry : map.entrySet()) {
                    sb.append(" • ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" FUN\n");
                }
                if (sb.length() == 0) sb.append("No financial data found.");

                sendResults(sb.toString());
            }
            // an prokeitai gia apotelesmata anazhthshs (List), stelnei th lista ws exei
            else {
                sendResults(finalResults);
            }

            // afairesh tou jobState apo th lista energwn jobs tou reducer
            synchronized (ReducerMain.activeJobs) {
                ReducerMain.activeJobs.remove(jobState.getRequestId());
            }

        } catch (InterruptedException e) {
            System.err.println("Monitor interrupted: " + e.getMessage());
        }
    }

    // methodos gia thn apostolh ths telikhs apanthshs ston master
    private void sendResults(Object results) {
        try (Socket s = new Socket(masterIP, masterPort);
             ObjectOutputStream output = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(s.getInputStream())) {

            // apostolh monadikou id kai twn apotelesmatwn
            String id = jobState.getRequestId();
            output.writeObject(new FinalResponse(id, results));
            output.flush();

            System.out.println("[MONITOR] Final results successfully sent to Master.");

        } catch (IOException e) {
            System.err.println("[MONITOR] Error while sending to Master: " + e.getMessage());
        }
    }
}