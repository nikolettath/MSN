package master;

import common.Game;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class MasterHandler extends Thread {
    private Socket clientSocket;
    private List<WorkerInfo> workers;

    //SRG Server info (melos 3)
    private static final String SRG_IP = "localhost";
    private static final int SRG_PORT = 5000;

    public MasterHandler(Socket socket, List<WorkerInfo> workers) {
        this.clientSocket = socket;
        this.workers = workers;
    }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

            //read object from client
            Object request = in.readObject();

            //case: object from Manager App
            if (request instanceof Game) {
                Game game = (Game) request;
                System.out.println("Ελήφθη νέο παιχνίδι προς καταχώρηση: " + game.getGameName());

                //update SRG server
                boolean srgRegistered = registerWithSRG(game.getGameName(), game.getHashKey());

                if (!srgRegistered) {
                    out.writeUTF("ΣΦΑΛΜΑ: Ο SRG Server δεν είναι διαθέσιμος. Ακύρωση καταχώρησης.");
                    out.flush();
                    return; //Return gia na exoume synepeia sta dedomena
                }

                //hashing & epilogh worker
                int workerIndex = Math.abs(game.getGameName().hashCode()) % workers.size();
                WorkerInfo selectedWorker = workers.get(workerIndex);

                System.out.println("Δρομολόγηση του '" + game.getGameName() + "' στον Worker " + workerIndex);

                //send game to worker
                boolean workerSuccess = forwardToWorker(game, selectedWorker);

                //update Manager App
                if (workerSuccess) {
                    out.writeUTF("ΕΠΙΤΥΧΙΑ: Το παιχνίδι καταχωρήθηκε σωστά στο σύστημα!");
                } else {
                    out.writeUTF("ΣΦΑΛΜΑ: Ο Worker (" + workerIndex + ") δεν ανταποκρίνεται.");
                }
                out.flush();
            }
            // TODO: Εδώ αργότερα θα βάλουμε το "else if" για να πιάνουμε τα request από τον Dummy Player

        } catch (Exception e) {
            System.err.println("Σφάλμα στον Master Handler: " + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException e) { /* Ignore */ }
        }
    }

    /**
     * Ανοίγουμε σύνδεση socket με τον SRG Server για την αποστολή της εντολής init
     */
    private boolean registerWithSRG(String gameName, String hashKey) {
        try (Socket srgSocket = new Socket(SRG_IP, SRG_PORT);
             ObjectOutputStream srgOut = new ObjectOutputStream(srgSocket.getOutputStream());
             ObjectInputStream srgIn = new ObjectInputStream(srgSocket.getInputStream())) {

            String command = "INIT_GAME|" + gameName + "|" + hashKey;

            srgOut.writeUTF(command);
            srgOut.flush();

            //waiting for OK from SRG Server about Producer threads
            String response = srgIn.readUTF();
            return response.equals("OK");

        } catch (IOException e) {
            System.err.println("Αποτυχία σύνδεσης με SRG (" + SRG_IP + ":" + SRG_PORT + ")");
            return false;
        }
    }

    /**
     * Προωθούμε το παιχνίδι στον Worker που έχει επιλεγεί
     */
    private boolean forwardToWorker(Game game, WorkerInfo worker) {
        try (Socket workerSocket = new Socket(worker.getIp(), worker.getPort());
             ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
             ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream())) {

            workerOut.writeObject(game);
            workerOut.flush();

            String response = workerIn.readUTF();
            return response.equals("OK");

        } catch (IOException e) {
            System.err.println("Αποτυχία σύνδεσης με Worker στη θύρα " + worker.getPort());
            return false;
        }
    }
}