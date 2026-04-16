package master;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Master {
    private static final int PORT = 4321;
    // Λίστα με τις πληροφορίες των Workers (IP και Port)
    private List<WorkerInfo> workers;

    public Master(int numWorkers) {
        this.workers = new ArrayList<>();
        // Παράδειγμα: Οι workers τρέχουν σε localhost σε συνεχόμενες θύρες
        for (int i = 0; i < numWorkers; i++) {
            workers.add(new WorkerInfo("127.0.0.1", 8081 + i));
        }
    }

    public void listen() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Master Node started on port " + PORT);
            while (true) {
                // Όπως στο ServerMain.java του εργαστηρίου
                Socket socket = serverSocket.accept();
                new MasterHandler(socket, workers).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int n = 3; // Αριθμός workers (δυναμικός)
        new Master(n).listen();
    }
}