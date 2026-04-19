package master;

import common.Game;
import request.FilterRequest;
import request.FinalResponse;
import request.ReportRequest;
import java.io.*;
import java.net.Socket;
import java.util.List;


public class MasterHandler extends Thread {

    private Socket clientSocket;
    private List<WorkerInfo> workers;

    // gia epikoinonia me SRG kai Reducer
    private static final String SRG_IP = "localhost";
    private static final int SRG_PORT = 9090;

    // orizoume pou trexei o reducer - xrhsimopoieitai sta requests pros tous workers)
    private static final String REDUCER_IP = "localhost";
    private static final int REDUCER_PORT = 5000;

    public MasterHandler(Socket socket, List<WorkerInfo> workers) {
        this.clientSocket = socket;
        this.workers = workers;
    }


    @Override
    public void run() {

        // try-with-resources gia automato kleisimo twn streams tou socket
        try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream()))
        {
            out.flush();

            while (true)
            {
                Object request = in.readObject();   // anagnwsh apo to socket ( apo client h apo reducer)

                // -------------lhpsh final results apo reducer-----------------

                // otan o reducer syndeetai ston master gia na epistrepsei th game list h thn oikonomikh report.
                if (request instanceof FinalResponse)
                {
                    FinalResponse response = (FinalResponse) request;
                    String id = response.getRequestId();
                    Object results = response.getResults();

                    ObjectOutputStream playerOut = Master.clientRegistry.get(id);   // anazhthsh arxikou ObjectOutputStream tou client mesw ID

                    if (playerOut != null)
                    {
                        // synchronized sto stream gia na einai h eggrafh thread safe
                        synchronized (playerOut)
                        {
                            playerOut.writeObject(results);
                            playerOut.flush();
                        }
                        // afairesi apo to registry meta thn oloklhrwsh ths apostolhs
                        Master.clientRegistry.remove(id);
                        System.out.println("[MASTER] Results (ID: " + id + ") forwarded successfully to the client.");
                    } else
                    {
                        System.err.println("[MASTER] Error: Request ID " + id + " not found in Registry.");
                    }
                    break;
                }

                // -----------------kataxwrhsh new game apo manager------------------

                else if (request instanceof Game)
                {
                    handleNewGame((Game) request, out);
                }

                //---------------entoles player (strings)--------------------------

                else if (request instanceof String)
                {
                    String command = (String) request;

                    //----BET----
                    if (command.startsWith("PLAYER_CMD|BET")) {
                        handleBetCommand(command, out);
                    }

                    //----SEARCH (MapReduce)----
                    else if (command.startsWith("PLAYER_CMD|SEARCH")) {
                        handleSearchCommand(command, out);
                    }

                    //----REPORT (MapReduce)----
                    else if (command.startsWith("MANAGER_CMD|REPORT")) {
                        handleReportCommand(command, out);
                    }
                }
            }
        } catch (EOFException e) {
            System.out.println("[MASTER] A client disconnected normally.");
        } catch (Exception e) {
            System.err.println("[MASTER] Error in MasterHandler: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try
            {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                // Paravlepsi sfalmatos kata to kleisimo
            }
        }
    }


    /**
     * Ylopoiisi tis 2is Diorthosis: Eksipno Parse kai Broadcast FilterRequest.
     */
    private void handleSearchCommand(String command, ObjectOutputStream out) throws IOException {
        String[] parts = command.split("\\|");
        String[] args = parts[1].split(" ");

        String category = null;
        String riskLevel = null;
        String provider = null;

        // Parsing twn filtrwn
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("low") || arg.equalsIgnoreCase("medium") || arg.equalsIgnoreCase("high")) {
                riskLevel = arg.toLowerCase();
            } else if (arg.equals("$") || arg.equals("$$") || arg.equals("$$$")) {
                category = arg;
            } else {
                provider = arg;
            }
        }


        // Dimiourgoume ena monadiko ID gia afto to sygekrimeno aitima anazitisis
        String reqId = java.util.UUID.randomUUID().toString();

        // Apothikevoume to ObjectOutputStream tou paikti sto registry tou Master wste na kseroume pou na steiloume ti lista otan tin etoimasei o Reducer
        Master.clientRegistry.put(reqId, out);

        System.out.println("[MASTER] Starting SEARCH (ID: " + reqId + "). Filters -> Risk: " + riskLevel + ", Category: " + category + ", Provider: " + provider);

        // Dimiourgia tou FilterRequest me to requestId
        FilterRequest filterReq = new FilterRequest(reqId, category, provider, riskLevel, REDUCER_IP, REDUCER_PORT);

        // Apostoli se OLOUS tous Workers (Map Phase)
        int successCount = 0;
        for (WorkerInfo w : workers) {
            if (sendToWorkerGeneric(filterReq, w)) {
                successCount++;
            }
        }

        System.out.println("[MASTER] Request distributed to " + successCount + " workers. Waiting for Reducer...");
    }


    /**
     * Ylopoiisi Aggregation Query: Broadcast ReportRequest se olous tous Workers.
     */
    private void handleReportCommand(String command, ObjectOutputStream out) throws IOException {

        String[] parts = command.split("\\|");
        String type = parts[1]; // Prepei na einai "BY_PROVIDER" h' "BY_PLAYER"

        // Dimiourgoume ena monadiko ID gia afto to sygekrimeno aitima anaforas
        String reqId = java.util.UUID.randomUUID().toString();

        // Apothikevoume to ObjectOutputStream sto registry
        Master.clientRegistry.put(reqId, out);

        System.out.println("[MASTER] Starting REPORT for: " + type + " (ID: " + reqId + ")");

        // Dimiourgia tou ReportRequest me to requestId
        ReportRequest reportReq = new ReportRequest(reqId, type, REDUCER_IP, REDUCER_PORT);

        // Apostoli se OLOUS tous Workers
        int successCount = 0;
        for (WorkerInfo w : workers) {
            if (sendToWorkerGeneric(reportReq, w)) {
                successCount++;
            }
        }

        System.out.println("[MASTER] Report request distributed to " + successCount + " workers. Waiting for Reducer...");
    }


    /**
     * Xeirismos Pontarismatos (Apostoli se ENAN sygekrimeno Worker vasei Hash).
     */
    private void handleBetCommand(String command, ObjectOutputStream out) throws IOException, ClassNotFoundException {

        String[] parts = command.split("\\|");
        String[] args = parts[1].split(" ");
        String gameName = args[1];
        String betAmount = args[2];

        // Hashing gia evresi tou swstou Worker
        int workerIndex = (gameName.hashCode() & 0x7FFFFFFF) % workers.size();
        WorkerInfo selectedWorker = workers.get(workerIndex);

        try (Socket workerSocket = new Socket(selectedWorker.getIp(), selectedWorker.getPort());
             ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
             ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream())) {

            workerOut.writeObject("BET|" + gameName + "|" + betAmount);
            workerOut.flush();

            String response = (String) workerIn.readObject();
            out.writeObject(response);
            out.flush();
        } catch (IOException e) {
            out.writeObject("ERROR: Worker " + workerIndex + " is not available.");
            out.flush();
        }
    }

    /**
     * Kataxwrisi neou paixnidiou (SRG Register -> Worker Forward).
     */
    private void handleNewGame(Game game, ObjectOutputStream out) throws IOException, ClassNotFoundException {

        System.out.println("Processing new game: " + game.getGameName());

        // eggrafi ston SRG
        if (!registerWithSRG(game.getGameName(), game.getHashKey())) {
            out.writeObject("ERROR: Failed to connect with SRG Server.");
            out.flush();
            return;
        }

        // epilogi Worker vasei Hash
        int workerIndex = Math.abs(game.getGameName().hashCode()) % workers.size();
        WorkerInfo selectedWorker = workers.get(workerIndex);

        // proothisi ston Worker
        if (sendToWorkerGeneric(game, selectedWorker)) {
            out.writeObject("SUCCESS: Game '" + game.getGameName() + "' saved on Worker " + workerIndex);
        } else {
            out.writeObject("ERROR: Failed to save on Worker " + workerIndex);
        }
        out.flush();
    }


    /**
     * Voithitiki methodos gia apostoli opoioudipote Serializable antikeimenou se Worker.
     */
    private boolean sendToWorkerGeneric(Serializable obj, WorkerInfo worker) {

        try (Socket s = new Socket(worker.getIp(), worker.getPort());
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {

            out.writeObject(obj);
            out.flush();

            Object response = in.readObject();
            return response != null; // Epistrefei true an o Worker apantise
        } catch (Exception e) {
            System.err.println("[MASTER] Failed to communicate with Worker on port " + worker.getPort());
            return false;
        }
    }


    /**
     * Voithitiki methodos gia tin eggrafi enos neou paixnidiou ston Secure Random Generator (SRG).
     * Stelnei to onoma kai to hash key kai perimenei epivevaiosi "OK".
     */
    private boolean registerWithSRG(String gameName, String hashKey) {

        try (Socket srgSocket = new Socket(SRG_IP, SRG_PORT);
             ObjectOutputStream srgOut = new ObjectOutputStream(srgSocket.getOutputStream());
             ObjectInputStream srgIn = new ObjectInputStream(srgSocket.getInputStream())) {

            srgOut.writeObject("REGISTER," + gameName + "," + hashKey);
            srgOut.flush();

            String response = (String) srgIn.readObject();
            return "OK".equals(response);
        } catch (Exception e) {
            return false;
        }
    }
}