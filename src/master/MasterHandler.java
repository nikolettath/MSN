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

<<<<<<< HEAD
    // Ρυθμίσεις Δικτύου (Localhost για τις δοκιμές σου)
=======
    // rythmiseis diktyou
    private static final String SRG_IP = "localhost";
>>>>>>> a062b66e9299524251c8dc15012372704f05bcc0
    private static final int SRG_PORT = 9090;
    private static final int REDUCER_PORT = 5000;
    private String srgIp;
    private String reducerIp;

    public MasterHandler(Socket socket, List<WorkerInfo> workers, String srgIp, String reducerIp) {
        this.clientSocket = socket;
        this.workers = workers;
        this.srgIp = srgIp;
        this.reducerIp = reducerIp;
    }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
            out.flush();

            while (true) {
                Object request = in.readObject();

                // diaxeirish apanthshs apo reducer
                if (request instanceof FinalResponse) {
                    FinalResponse response = (FinalResponse) request;
                    String id = response.getRequestId();
                    ObjectOutputStream playerOut;
                    synchronized (Master.clientRegistry) {
                        playerOut = Master.clientRegistry.get(id);
                    }

                    if (playerOut != null) {
                        synchronized (playerOut) {
                            playerOut.writeObject(response.getResults());
                            playerOut.flush();
                        }
                        synchronized (Master.clientRegistry) {
                            Master.clientRegistry.remove(id);
                        }
                    }
                    break;
                }
                // diaxeirish neou paixnidiou
                else if (request instanceof Game) {
                    handleNewGame((Game) request, out);
                }
                // diaxeirish entolwn keimenou
                else if (request instanceof String) {
                    String command = (String) request;

                    if (command.startsWith("PLAYER_CMD|BET")) handleBetCommand(command, out);
                    else if (command.startsWith("PLAYER_CMD|SEARCH")) handleSearchCommand(command, out);
                    else if (command.startsWith("PLAYER_CMD|RATE")) handleRateCommand(command, out);
                    else if (command.startsWith("MANAGER_CMD|REPORT")) handleReportCommand(command, out);
                    else if (command.startsWith("MANAGER_CMD|REMOVE")) handleRemoveCommand(command, out);
                    else if (command.startsWith("MANAGER_CMD|RESTORE")) handleRestoreCommand(command, out);
                    else if (command.startsWith("MANAGER_CMD|EDIT_RISK")) handleEditRiskCommand(command, out);
                    else if (command.startsWith("MANAGER_CMD|EDIT_LIMITS")) handleEditLimitsCommand(command, out);
                }
            }
        } catch (EOFException e) {
            // fysiologikh aposyndesh
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (!clientSocket.isClosed()) clientSocket.close(); } catch (IOException e) { }
        }
    }

    // methodoi diaxeirishs

    private void handleSearchCommand(String command, ObjectOutputStream out) throws IOException {
        String[] parts = command.split("\\|");
        String riskLevel = parts[2].equals("ANY") ? null : parts[2];
        String category = parts[3].equals("ANY") ? null : parts[3];
        String provider = parts[4].equals("ANY") ? null : parts[4];
        int minStars = Integer.parseInt(parts[5]);

        // dhmiourgia monadikou id
        String reqId = java.util.UUID.randomUUID().toString();
        synchronized (Master.clientRegistry) {
            Master.clientRegistry.put(reqId, out);
        }

<<<<<<< HEAD
        FilterRequest filterReq = new FilterRequest(reqId, category, provider, riskLevel, minStars, this.reducerIp, REDUCER_PORT);
=======
        // apostolh aithmatos se olous tous workers
        FilterRequest filterReq = new FilterRequest(reqId, category, provider, riskLevel, minStars, REDUCER_IP, REDUCER_PORT);
>>>>>>> a062b66e9299524251c8dc15012372704f05bcc0
        for (WorkerInfo w : workers) {
            sendToWorkerGeneric(filterReq, w);
        }
    }

    private void handleReportCommand(String command, ObjectOutputStream out) throws IOException {
        String reqId = java.util.UUID.randomUUID().toString();
        synchronized (Master.clientRegistry) {
            Master.clientRegistry.put(reqId, out);
        }
<<<<<<< HEAD
        ReportRequest reportReq = new ReportRequest(reqId, command.split("\\|")[2], this.reducerIp, REDUCER_PORT);
=======

        // apostolh aithmatos report se olous tous workers
        ReportRequest reportReq = new ReportRequest(reqId, command.split("\\|")[2], REDUCER_IP, REDUCER_PORT);
>>>>>>> a062b66e9299524251c8dc15012372704f05bcc0
        for (WorkerInfo w : workers) {
            sendToWorkerGeneric(reportReq, w);
        }
    }

    private void handleNewGame(Game game, ObjectOutputStream out) throws IOException {
<<<<<<< HEAD
        game.setSrgIp(this.srgIp);

=======
        // eggrafh sto srg
>>>>>>> a062b66e9299524251c8dc15012372704f05bcc0
        if (!registerWithSRG(game.getGameName(), game.getHashKey())) {
            out.writeObject("ERROR: Failed to connect with SRG Server.");
            out.flush();
            return;
        }

        // ypologismos primary kai replica worker
        int primaryIndex = (game.getGameName().hashCode() & 0x7FFFFFFF) % workers.size();
        int replicaIndex = (primaryIndex + 1) % workers.size();

        boolean primarySuccess = sendToWorkerGeneric(game, workers.get(primaryIndex));
        boolean replicaSuccess = sendToWorkerGeneric(game, workers.get(replicaIndex));

        if (primarySuccess && replicaSuccess) out.writeObject("SUCCESS: Saved on Primary & Replica!");
        else if (primarySuccess || replicaSuccess) out.writeObject("WARNING: Saved on 1 Worker only.");
        else out.writeObject("ERROR: Failed to save on any Worker.");
        out.flush();
    }

    // fault tolerance kai replication

    // strathgikh bet: primary h replica an pesei
    private String sendToReplicaGroup(String gameName, String command) {
        int primaryIndex = (gameName.hashCode() & 0x7FFFFFFF) % workers.size();
        int replicaIndex = (primaryIndex + 1) % workers.size();

        // prospatheia ston primary
        try (Socket s = new Socket(workers.get(primaryIndex).getIp(), workers.get(primaryIndex).getPort());
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeObject(command);
            out.flush();
            return (String) in.readObject();
        } catch (Exception e1) {
            System.err.println("[MASTER] Primary " + primaryIndex + " DOWN. Routing to Replica " + replicaIndex + "...");
            // prospatheia ston replica
            try (Socket s = new Socket(workers.get(replicaIndex).getIp(), workers.get(replicaIndex).getPort());
                 ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
                out.writeObject(command);
                out.flush();
                return (String) in.readObject();
            } catch (Exception e2) {
                return "ERROR|Both Primary and Replica Workers are offline!";
            }
        }
    }

    // strathgikh enhmerwshs: kai stous dyo workers
    private String updateBothReplicas(String gameName, String command) {
        int primaryIndex = (gameName.hashCode() & 0x7FFFFFFF) % workers.size();
        int replicaIndex = (primaryIndex + 1) % workers.size();

        String primaryResponse = null;
        String replicaResponse = null;

        // enhmerwsh primary
        try (Socket s = new Socket(workers.get(primaryIndex).getIp(), workers.get(primaryIndex).getPort());
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeObject(command); out.flush();
            primaryResponse = (String) in.readObject();
        } catch (Exception e) { System.err.println("[MASTER] Primary " + primaryIndex + " is DOWN."); }

        // enhmerwsh replica
        try (Socket s = new Socket(workers.get(replicaIndex).getIp(), workers.get(replicaIndex).getPort());
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeObject(command); out.flush();
            replicaResponse = (String) in.readObject();
        } catch (Exception e) { System.err.println("[MASTER] Replica " + replicaIndex + " is DOWN."); }

        if (primaryResponse != null) return primaryResponse;
        if (replicaResponse != null) return replicaResponse;
        return "ERROR|Both Primary and Replica Workers are offline!";
    }

    // command handlers

    private void handleRateCommand(String command, ObjectOutputStream out) throws IOException {
        out.writeObject(updateBothReplicas(command.split("\\|")[2], command)); out.flush();
    }

    private void handleRemoveCommand(String command, ObjectOutputStream out) throws IOException {
        out.writeObject(updateBothReplicas(command.split("\\|")[2], command)); out.flush();
    }

    private void handleRestoreCommand(String command, ObjectOutputStream out) throws IOException {
        out.writeObject(updateBothReplicas(command.split("\\|")[2], command)); out.flush();
    }

    private void handleEditRiskCommand(String command, ObjectOutputStream out) throws IOException {
        out.writeObject(updateBothReplicas(command.split("\\|")[2], command)); out.flush();
    }

    private void handleEditLimitsCommand(String command, ObjectOutputStream out) throws IOException {
        out.writeObject(updateBothReplicas(command.split("\\|")[2], command)); out.flush();
    }

    private void handleBetCommand(String command, ObjectOutputStream out) throws IOException {
        String[] parts = command.split("\\|");
        String gameName = parts[2];
        double betAmount = Double.parseDouble(parts[3]);

        String workerCommand = "BET|" + gameName + "|" + betAmount + "|" + parts[4];
        String response = sendToReplicaGroup(gameName, workerCommand);

        if (response.startsWith("PAYOUT|")) {
            String[] resParts = response.split("\\|");
            // synoliko poso epistrofhs
            double wonAmount = Double.parseDouble(resParts[1]);
            String msg = resParts[2];
            // apostolh apotelesmatos ston paikth
            out.writeObject("PAYOUT_RESULT|" + wonAmount + "|" + msg);
        }
        else {
            // epistrofh xrhmatwn se sfalma
            out.writeObject("REFUND|" + betAmount + "|" + response.replace("ERROR|", ""));
        }
        out.flush();
    }

    // voithitikes methodoi

    private boolean sendToWorkerGeneric(Serializable obj, WorkerInfo worker) {
        try (Socket s = new Socket(worker.getIp(), worker.getPort());
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeObject(obj);
            out.flush();
            return in.readObject() != null;
        } catch (Exception e) { return false; }
    }

    private boolean registerWithSRG(String gameName, String hashKey) {
        try (Socket s = new Socket(this.srgIp, SRG_PORT);
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeObject("REGISTER," + gameName + "," + hashKey);
            out.flush();
            return "OK".equals(in.readObject());
        } catch (Exception e) { return false; }
    }
}