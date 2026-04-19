package reducer;

import common.Game;
import jobState.JobState;

import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ReducerThread extends Thread {

    private final Socket socket;
    private final JobState jobState;

    // Ο Constructor πλέον δέχεται ΜΟΝΟ το ενοποιημένο JobState
    public ReducerThread(Socket socket, JobState jobState) {
        this.socket = socket;
        this.jobState = jobState;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {

        try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream()))
        {
            // reading data sent by worker
            Object data = input.readObject();

            // case 1: data is a list of games (filter request)
            if (data instanceof List<?>)
            {
                List<Game> partialResult = (List<Game>) data;
                jobState.addWorkerResult(partialResult);
            }
            // case 2: data is a map of financial statistics (report request)
            else if (data instanceof Map<?, ?>)
            {
                Map<String, Double> partialReport = (Map<String, Double>) data;
                jobState.addWorkerReport(partialReport);
            }
            // case 3: unknown data type
            else
            {
                System.out.println("[REDUCER] Unknown data format received.");
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