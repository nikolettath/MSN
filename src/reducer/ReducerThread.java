package reducer;

import common.Game;
import jobState.JobState;
import monitor.ResultMonitor;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ReducerThread extends Thread {
    private final Socket socket;
    private final int sumWorkers;
    private final String masterIP;
    private final int masterPort;
    private final boolean isReport;

    public ReducerThread(Socket socket, int sumWorkers, String masterIP, int masterPort, boolean isReport) {
        this.socket = socket;
        this.sumWorkers = sumWorkers;
        this.masterIP = masterIP;
        this.masterPort = masterPort;
        this.isReport = isReport;
    }


    @Override
    public void run() {
        try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream()))
        {
            Object incoming = input.readObject();

            if (incoming instanceof Object[])
            {
                Object[] packet = (Object[]) incoming;
                String reqId = (String) packet[0];
                Object data = packet[1];

                // an den yparxei jobState gia to ID to dhmiourgoume kai xekiname to monitor
                JobState jobState = ReducerMain.activeJobs.computeIfAbsent(reqId, id -> {
                    JobState newJob = new JobState(id, sumWorkers);
                    new ResultMonitor(newJob, masterIP, masterPort, isReport).start();
                    return newJob;
                });

                // prosthhkh dedomenwn sto swsto JobState
                if (data instanceof List<?>)
                {
                    jobState.addWorkerResult((List<Game>) data);
                } else if (data instanceof Map<?, ?>)
                {
                    jobState.addWorkerReport((Map<String, Double>) data);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error reading from Worker: " + e.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}