/*package reducer;

import jobState.JobState;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ReducerMain {

    // hashmap me ta energa jobs RequestID -> JobState
    public static final ConcurrentHashMap<String, JobState> activeJobs = new ConcurrentHashMap<>();


    public static void main(String[] args) {

        if (args.length < 5) {
            System.err.println("Use of: java ReducerMain <port> <sum_workers> <master_ip> <master_port> <is_report(true/false)>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        int sumWorkers = Integer.parseInt(args[1]);
        String masterIP = args[2];
        int masterPort = Integer.parseInt(args[3]);
        boolean isReport = Boolean.parseBoolean(args[4]);

        try (ServerSocket serverSocket = new ServerSocket(port))
        {
            System.out.println("Reducer started at port: " + port + "\n");
            System.out.println("Waiting for " + sumWorkers + " Workers. (Report Mode: " + isReport + ")");

            while (true) {
                Socket workerSocket = serverSocket.accept();

                // dhmiourgoume to thread pernontas oles tis parametrous
                ReducerThread t = new ReducerThread(workerSocket, sumWorkers, masterIP, masterPort, isReport);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Error in Reducer: " + e.getMessage());
        }
    }
}*/
package reducer;

import jobState.JobState;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReducerMain {

    // KANONIKO HashMap (oxi Concurrent). O sygxronismos tha ginetai me synchronized.
    public static final Map<String, JobState> activeJobs = new HashMap<>();

    public static void main(String[] args) {

        if (args.length < 5) {
            System.err.println("Use of: java ReducerMain <port> <sum_workers> <master_ip> <master_port> <is_report(true/false)>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        int sumWorkers = Integer.parseInt(args[1]);
        String masterIP = args[2];
        int masterPort = Integer.parseInt(args[3]);
        boolean isReport = Boolean.parseBoolean(args[4]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Reducer started at port: " + port + "\n");
            System.out.println("Waiting for " + sumWorkers + " Workers. (Report Mode: " + isReport + ")");

            while (true) {
                Socket workerSocket = serverSocket.accept();

                // dhmiourgoume to thread pernontas oles tis parametrous
                ReducerThread t = new ReducerThread(workerSocket, sumWorkers, masterIP, masterPort, isReport);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Error in Reducer: " + e.getMessage());
        }
    }
}