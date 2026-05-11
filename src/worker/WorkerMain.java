package worker;

import common.MemoryStorage;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;

public class WorkerMain {

    public static void main(String[] args) {

        // dymanikh apodosh ths portas kata thn ekkinhsh
        if (args.length < 1) {
            System.err.println("Use of: java WorkerMain <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);

        // dhmiourgia koinhs mnhmhs (RAM storage) gia ton worker
        MemoryStorage ms = new MemoryStorage();

        // ekkinhsh tou tcp server gia ton worker
        try (ServerSocket serverSocket = new ServerSocket(port))
        {
            System.out.println("Worker successfully started at port: " + port);

            // loop gia thn apodoxh aithmatwn apo ton master
            while (true)
            {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from Master: " + clientSocket.getInetAddress());

                // dhmiourgia neou thread gia thn eksyphrethsh kathe aithmatos
                WorkerThread t = new WorkerThread(clientSocket, ms);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Error starting Worker: " + e.getMessage());
        }
    }
}