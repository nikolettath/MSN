package master;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Master {

    private static final int PORT = 4321;
    private List<WorkerInfo> workers;

    // Registry για την αποστολή αποτελεσμάτων στους clients
    public static final Map<String, ObjectOutputStream> clientRegistry = new HashMap<>();

    // ΝΕΟ: Registry για τα πορτοφόλια (Balances) των παικτών
    public static final Map<String, Double> playerBalances = new HashMap<>();

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
        List<WorkerInfo> dynamicWorkers = new ArrayList<>();
        File configFile = new File("workers_config.txt");

        // Diavazei ta IPs twn Workers dynamika apo to config file
        if (configFile.exists()) {
            System.out.println("[MASTER] Reading workers from config file...");
            try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        String[] parts = line.split(":");
                        String ip = parts[0];
                        int port = Integer.parseInt(parts[1]);
                        dynamicWorkers.add(new WorkerInfo(ip, port));
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading config file: " + e.getMessage());
            }
        } else {
            System.out.println("[!] Config file not found. Falling back to default (1 localhost worker).");
            dynamicWorkers.add(new WorkerInfo("127.0.0.1", 8081));
        }

        if (dynamicWorkers.isEmpty()) {
            System.err.println("[!] No workers configured. Exiting.");
            System.exit(1);
        }

        Master master = new Master(dynamicWorkers);
        master.listen();
    }
}