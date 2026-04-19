package worker;

import common.MemoryStorage;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;

public class WorkerMain {

    public static void main(String[] args) {

        // dynamically appointing of port during initialization
        if (args.length < 1) {
            System.err.println("Use of: java WorkerMain <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);

        // creating shared memory storage for worker
        MemoryStorage ms = new MemoryStorage();

        // starting ServerSocket
        try (ServerSocket serverSocket = new ServerSocket(port))
        {
            System.out.println("Worker successfully started at port: " + port);

            // endless loop to constantly receive requests
            while (true)
            {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from Master: " + clientSocket.getInetAddress());

                // for every request (client) open new thread
                WorkerThread t = new WorkerThread(clientSocket, ms);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Error starting Worker: " + e.getMessage());
        }
    }
}