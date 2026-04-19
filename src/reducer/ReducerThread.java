package reducer;

import common.Game;
import jobState.JobState;
import jobState.ReportJobState;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ReducerThread extends Thread {

    private final Socket socket;
    private final JobState jobState;
    private final ReportJobState reportJobState; // added for financial reports


    public ReducerThread(Socket socket, JobState jobState, ReportJobState reportJobState) {
        this.socket = socket;
        this.jobState = jobState;
        this.reportJobState = reportJobState;
    }


    @Override
    public void run() {

        try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream()))
        {
            // reading game list /map result sent by worker
            Object data = input.readObject();

            // case 1: data is a list of games (filter request)
            if (data instanceof List<?>) {
                List<Game> result = (List<Game>) data;

                // forward list to JobState for safe merge
                jobState.addWorkerResult(result);
            }
            // case 2: data is a map of financial statistics (report request)
            else if (data instanceof Map<?, ?>) {
                Map<String, Double> report = (Map<String, Double>) data;

                // forward map to ReportJobState for safe merge
                reportJobState.addWorkerReport(report);
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error reading from Worker: " + e.getMessage());

        } finally
        {
            try
            {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}