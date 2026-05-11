package reducer;

import jobState.JobState;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReducerMain {

    // xrhsh aplou map, o sygxronismos ginetai mesw synchronized
    public static final Map<String, JobState> activeJobs = new HashMap<>();

    public static void main(String[] args) {

        // elegxos aparaithtwn orismatwn
        if (args.length < 4) {
            System.err.println("Use of: java ReducerMain <port> <sum_workers> <master_ip> <master_port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        int sumWorkers = Integer.parseInt(args[1]);
        String masterIP = args[2];
        int masterPort = Integer.parseInt(args[3]);

        // ekkinhsh tcp server tou reducer
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Smart Reducer started at port: " + port + "\n");
            System.out.println("Waiting for " + sumWorkers + " Workers. (Unified MapReduce Mode)");

            // loop pou asygxrona apodexetai syndeseis apo tous workers
            while (true) {
                Socket workerSocket = serverSocket.accept();

                // dhmiourgia thread gia thn eksyphrethsh kathe neou worker
                ReducerThread t = new ReducerThread(workerSocket, sumWorkers, masterIP, masterPort);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Error in Reducer: " + e.getMessage());
        }
    }
}