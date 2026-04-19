package reducer;

import common.Game;
import jobState.JobState;

import java.net.Socket;
import java.util.List;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ReducerThread extends Thread {

    private final Socket socket;
    private final JobState jobState;


    public ReducerThread(Socket socket, JobState jobState) {
        this.socket = socket;
        this.jobState = jobState;
    }


    @Override
    public void run() {

        try (ObjectInputStream input = new ObjectInputStream(socket.getInputStream()))
        {
            // reading game list /map result sent by worker
            Object data = input.readObject();

            if (data instanceof List<?>) {
                List<Game> result = (List<Game>) data;

                // forward list to JobState for safe merge
                jobState.addWorkerResult(result);
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