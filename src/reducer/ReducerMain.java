package reducer;

import jobState.JobState;
import monitor.ResultMonitor;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;

public class ReducerMain {

    public static void main(String[] args) {

        // dynamically reading port and total number of workers
        if (args.length < 2) {
            System.err.println("Use of: java ReducerMain <port> <sum_workers>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        int sumWorkers = Integer.parseInt(args[1]);

        // creating shared state for job
        JobState jobState = new JobState(sumWorkers);

        // starting monitor thread to wait for results
        new ResultMonitor(jobState).start();

        try (ServerSocket serverSocket = new ServerSocket(port))
        {
            System.out.println("Reducer started at port: " + port + "\n");
            System.out.println("Waiting for " + sumWorkers + " Workers.");

            while (true)
            {
                Socket workerSocket = serverSocket.accept();
                System.out.println("New connection from Worker: " + workerSocket.getInetAddress());

                // for every worker connected we open a ReducerThread
                ReducerThread t = new ReducerThread(workerSocket, jobState);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Error in Reducer: " + e.getMessage());
        }
    }
}
