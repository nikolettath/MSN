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
    // lista me ta stoixeia diktyou twn energwn workers
    private List<WorkerInfo> workers;
    private String srgIp;
    private String reducerIp;

    // registry gia thn proswrinh apothhkeysh tou stream apanthshs ston client (xrhsh requestId)
    public static final Map<String, ObjectOutputStream> clientRegistry = new HashMap<>();

    // Ο Constructor τώρα παίρνει και τις IPs των άλλων κόμβων!
    public Master(List<WorkerInfo> workers, String srgIp, String reducerIp) {
        this.workers = workers;
        this.srgIp = srgIp;
        this.reducerIp = reducerIp;
    }

    public void listen() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Master Node started on port: " + PORT);
            System.out.println("SRG Server IP: " + srgIp);
            System.out.println("Reducer Node IP: " + reducerIp);
            System.out.println("Connected Workers: " + workers.size());
            for (int i = 0; i < workers.size(); i++) {
                System.out.println("   Worker " + i + ": " + workers.get(i).getIp() + ":" + workers.get(i).getPort());
            }
            // kyklogyrikos elenxos gia nees syndeseis
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[MASTER] New client connection from: " + socket.getInetAddress());

                // dimiourgia neou thread gia kathe epikoinwnia me manager h paikth
                // Περνάμε τις δυναμικές IPs στον Handler!
                new MasterHandler(socket, workers, srgIp, reducerIp).start();
            }
        } catch (IOException e) { System.err.println("Error: " + e.getMessage()); }
    }

    public static void main(String[] args) {
        // dynamic workers initialization (xwris na einai hardcoded)
        List<WorkerInfo> dynamicWorkers = new ArrayList<>();
        File configFile = new File("workers_config.txt");

        // Προεπιλεγμένες τιμές σε περίπτωση που λείπουν
        String parsedSrgIp = "127.0.0.1";
        String parsedReducerIp = "127.0.0.1";

        if (configFile.exists()) {
            System.out.println("[MASTER] Reading config from file...");
            // diavasma tou configuration file grammh grammh
            try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    // agnohse tis kenes grammes kai ta comments
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    if (line.startsWith("SRG_IP=")) {
                        parsedSrgIp = line.substring(7).trim();
                    } else if (line.startsWith("REDUCER_IP=")) {
                        parsedReducerIp = line.substring(11).trim();
                    } else if (line.startsWith("WORKER=")) {
                        String[] parts = line.substring(7).trim().split(":");
                        dynamicWorkers.add(new WorkerInfo(parts[0], Integer.parseInt(parts[1])));
                    }
                }
            } catch (IOException e) { System.err.println("Error reading config file: " + e.getMessage()); }
        } else {
            // fallback (se periptwsh pou den vrethei config file)
            System.out.println("[!] Config file not found. Defaulting to 1 localhost worker.");
            dynamicWorkers.add(new WorkerInfo("127.0.0.1", 8081));
        }

        // termatismos an de vrethei oute enas worker sto list
        if (dynamicWorkers.isEmpty()) {
            System.err.println("[!] No workers found. Exiting...");
            System.exit(1);
        }

        new Master(dynamicWorkers, parsedSrgIp, parsedReducerIp).listen();
    }
}