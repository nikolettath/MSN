package master;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Master {

    private static final int PORT = 4321;
    private List<WorkerInfo> workers;

    public static final Map<String, ObjectOutputStream> clientRegistry = new ConcurrentHashMap<>();

    public Master(List<WorkerInfo> workers) {
        this.workers = workers;
    }


    public void listen() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Master Node started on port: " + PORT);
            System.out.println("Connected Workers: " + workers.size());
            for (int i = 0; i < workers.size(); i++) {
                System.out.println("   Worker " + i + ": " + workers.get(i).getIp() + ":" + workers.get(i).getPort());
            }

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[MASTER] New client connection from: " + socket.getInetAddress());

                // dhmiourgia neou thread gia kathe client
                new MasterHandler(socket, workers).start();
            }
        } catch (IOException e) {
            System.err.println("Error while starting ServerSocket on Master: " + e.getMessage());
        }
    }


    public static void main(String[] args) {

        // elenxos an dothhke o arithmos twn workers ws orisma
        if (args.length < 1)
        {
            System.out.println("Usage: java master.Master <number_of_workers>");
            System.out.println("Example: java master.Master 3");

            System.out.println("\n[!] No arguments provided. Starting with default 3 workers.");
            args = new String[]{"3"};
        }

        int numWorkers = Integer.parseInt(args[0]);
        List<WorkerInfo> dynamicWorkers = new ArrayList<>();

        // dynamikh dhmiourgia twn WorkerInfo.
        // ypothetoume oti oi Workers trexoun sto localhost ksekinontas apo to port 8081.
        for (int i = 0; i < numWorkers; i++) {
            int workerPort = 8081 + i;
            dynamicWorkers.add(new WorkerInfo("127.0.0.1", workerPort));
        }

        // ekkinisi tou Master me th dynamic list
        Master master = new Master(dynamicWorkers);
        master.listen();
    }
}