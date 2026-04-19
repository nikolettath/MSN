package reducer;

import jobState.JobState;
import monitor.ResultMonitor;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;

public class ReducerMain {

    public static void main(String[] args) {

        // Τώρα διαβάζουμε 5 ορίσματα για να ξέρει ο Reducer πού να στείλει τα αποτελέσματα και τι είδους είναι
        if (args.length < 5) {
            System.err.println("Use of: java ReducerMain <port> <sum_workers> <master_ip> <master_port> <is_report(true/false)>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        int sumWorkers = Integer.parseInt(args[1]);
        String masterIP = args[2];
        int masterPort = Integer.parseInt(args[3]);
        boolean isReport = Boolean.parseBoolean(args[4]);

        // 1. Δημιουργούμε ΜΟΝΟ ΕΝΑ ενοποιημένο JobState
        JobState jobState = new JobState(sumWorkers);

        // 2. Ξεκινάμε το Monitor περνώντας του τα νέα ορίσματα
        new ResultMonitor(jobState, masterIP, masterPort, isReport).start();

        try (ServerSocket serverSocket = new ServerSocket(port))
        {
            System.out.println("Reducer started at port: " + port + "\n");
            System.out.println("Waiting for " + sumWorkers + " Workers. (Report Mode: " + isReport + ")");

            while (true)
            {
                Socket workerSocket = serverSocket.accept();
                System.out.println("New connection from Worker: " + workerSocket.getInetAddress());

                // 3. Για κάθε worker που συνδέεται, ανοίγουμε ένα ReducerThread περνώντας ΜΟΝΟ το jobState
                ReducerThread t = new ReducerThread(workerSocket, jobState);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Error in Reducer: " + e.getMessage());
        }
    }
}