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

    // arxikopoihsh parametrwn
    public ReducerThread(Socket socket, int sumWorkers, String masterIP, int masterPort) {
        this.socket = socket;
        this.sumWorkers = sumWorkers;
        this.masterIP = masterIP;
        this.masterPort = masterPort;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
            Object incoming = input.readObject();

            if (incoming instanceof Object[]) {
                Object[] packet = (Object[]) incoming;
                String reqId = (String) packet[0];
                Object data = packet[1];

                JobState jobState;

                // sygxronismos panw sto map twn energwn jobs
                synchronized (ReducerMain.activeJobs) {
                    jobState = ReducerMain.activeJobs.get(reqId);
                    // an den yparxei to dhmioyrgei kai to apothhkevei
                    if (jobState == null) {
                        jobState = new JobState(reqId, sumWorkers);
                        ReducerMain.activeJobs.put(reqId, jobState);
                        // ksekinaei to monitor thread gia to sygkekrimeno request
                        new ResultMonitor(jobState, masterIP, masterPort).start();
                    }
                }

                // prosthkh twn dedomenwn (anazhthsh h report) sto state tou job
                if (data instanceof List<?>) {
                    jobState.addWorkerResult((List<Game>) data);
                } else if (data instanceof Map<?, ?>) {
                    jobState.addWorkerReport((Map<String, Double>) data);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error reading from Worker: " + e.getMessage());
        } finally {
            // asfalhs termatismos syndeshs me ton worker
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}