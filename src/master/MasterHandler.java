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

    private static final String SRG_IP = "localhost";
    private static final int SRG_PORT = 9090;

    private static final String REDUCER_IP = "localhost";
    private static final int REDUCER_PORT = 5000;

    public MasterHandler(Socket socket, List<WorkerInfo> workers) {
        this.clientSocket = socket;
        this.workers = workers;
    }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
            out.flush();

            while (true) {
                Object request = in.readObject();

                if (request instanceof FinalResponse) {
                    FinalResponse response = (FinalResponse) request;
                    String id = response.getRequestId();
                    Object results = response.getResults();

                    ObjectOutputStream playerOut;
                    synchronized (Master.clientRegistry) {
                        playerOut = Master.clientRegistry.get(id);
                    }

                    if (playerOut != null) {
                        synchronized (playerOut) {
                            playerOut.writeObject(results);
                            playerOut.flush();
                        }
                        synchronized (Master.clientRegistry) {
                            Master.clientRegistry.remove(id);
                        }
                    }
                    break;
                }

                else if (request instanceof Game) {
                    handleNewGame((Game) request, out);
                }

                else if (request instanceof String) {
                    String command = (String) request;

                    if (command.startsWith("PLAYER_CMD|BET")) {
                        handleBetCommand(command, out);
                    }
                    else if (command.startsWith("PLAYER_CMD|SEARCH")) {
                        handleSearchCommand(command, out);
                    }
                    else if (command.startsWith("PLAYER_CMD|ADD_BALANCE")) {
                        handleAddBalanceCommand(command, out);
                    }
                    else if (command.startsWith("PLAYER_CMD|RATE")) {
                        handleRateCommand(command, out);
                    }
                    else if (command.startsWith("MANAGER_CMD|REPORT")) {
                        handleReportCommand(command, out);
                    }
                    else if (command.startsWith("MANAGER_CMD|REMOVE")) {
                        handleRemoveCommand(command, out);
                    }
                    else if (command.startsWith("MANAGER_CMD|EDIT_RISK")) {
                        handleEditRiskCommand(command, out);
                    }
                }
            }
        } catch (EOFException e) {
            // Normal disconnect
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (!clientSocket.isClosed()) clientSocket.close(); } catch (IOException e) { }
        }
    }

    private void handleSearchCommand(String command, ObjectOutputStream out) throws IOException {
        String[] parts = command.split("\\|");
        String riskLevel = parts[2].equals("ANY") ? null : parts[2];
        String category = parts[3].equals("ANY") ? null : parts[3];
        String provider = parts[4].equals("ANY") ? null : parts[4];
        int minStars = Integer.parseInt(parts[5]);

        String reqId = java.util.UUID.randomUUID().toString();

        synchronized (Master.clientRegistry) {
            Master.clientRegistry.put(reqId, out);
        }

        FilterRequest filterReq = new FilterRequest(reqId, category, provider, riskLevel, minStars, REDUCER_IP, REDUCER_PORT);

        for (WorkerInfo w : workers) {
            sendToWorkerGeneric(filterReq, w);
        }
    }

    private void handleReportCommand(String command, ObjectOutputStream out) throws IOException {
        String[] parts = command.split("\\|");
        String type = parts[1];
        String reqId = java.util.UUID.randomUUID().toString();

        synchronized (Master.clientRegistry) {
            Master.clientRegistry.put(reqId, out);
        }

        ReportRequest reportReq = new ReportRequest(reqId, type, REDUCER_IP, REDUCER_PORT);
        for (WorkerInfo w : workers) {
            sendToWorkerGeneric(reportReq, w);
        }
    }

    private void handleAddBalanceCommand(String command, ObjectOutputStream out) throws IOException {
        String[] parts = command.split("\\|");
        String username = parts[2];
        double amount = Double.parseDouble(parts[3]);

        synchronized (Master.playerBalances) {
            double current = Master.playerBalances.getOrDefault(username, 0.0);
            Master.playerBalances.put(username, current + amount);
            out.writeObject("SUCCESS: Added " + amount + " FUN. New balance: " + (current + amount) + " FUN.");
            out.flush();
        }
    }

    // ==========================================
    // BONUS: ACTIVE REPLICATION & FAULT TOLERANCE
    // ==========================================

    private void handleNewGame(Game game, ObjectOutputStream out) throws IOException, ClassNotFoundException {
        if (!registerWithSRG(game.getGameName(), game.getHashKey())) {
            out.writeObject("ERROR: Failed to connect with SRG Server.");
            out.flush();
            return;
        }

        // Υπολογισμός Primary και Replica Worker
        int primaryIndex = (game.getGameName().hashCode() & 0x7FFFFFFF) % workers.size();
        int replicaIndex = (primaryIndex + 1) % workers.size(); // Ο επόμενος Worker στον κύκλο

        WorkerInfo primaryWorker = workers.get(primaryIndex);
        WorkerInfo replicaWorker = workers.get(replicaIndex);

        boolean primarySuccess = sendToWorkerGeneric(game, primaryWorker);
        boolean replicaSuccess = sendToWorkerGeneric(game, replicaWorker);

        if (primarySuccess && replicaSuccess) {
            out.writeObject("SUCCESS: Game '" + game.getGameName() + "' saved on Primary Worker " + primaryIndex + " & Replica Worker " + replicaIndex);
        } else if (primarySuccess || replicaSuccess) {
            out.writeObject("WARNING: Game saved on only 1 Worker (Replication failed partially).");
        } else {
            out.writeObject("ERROR: Failed to save game on any Worker.");
        }
        out.flush();
    }

    // Βοηθητική μέθοδος για να "χτυπάει" τον Primary και αν πέσει να πάει στον Replica (Fault Tolerance)
    private String sendToReplicaGroup(String gameName, String command) {
        int primaryIndex = (gameName.hashCode() & 0x7FFFFFFF) % workers.size();
        int replicaIndex = (primaryIndex + 1) % workers.size();

        WorkerInfo primaryWorker = workers.get(primaryIndex);
        WorkerInfo replicaWorker = workers.get(replicaIndex);

        // 1η Προσπάθεια: Primary
        try (Socket workerSocket = new Socket(primaryWorker.getIp(), primaryWorker.getPort());
             ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
             ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream())) {

            workerOut.writeObject(command);
            workerOut.flush();
            return (String) workerIn.readObject();

        } catch (Exception e1) {
            System.err.println("[MASTER] Primary Worker " + primaryIndex + " is DOWN. Routing to Replica " + replicaIndex + "...");

            // 2η Προσπάθεια: Replica
            try (Socket workerSocket = new Socket(replicaWorker.getIp(), replicaWorker.getPort());
                 ObjectOutputStream workerOut = new ObjectOutputStream(workerSocket.getOutputStream());
                 ObjectInputStream workerIn = new ObjectInputStream(workerSocket.getInputStream())) {

                workerOut.writeObject(command);
                workerOut.flush();
                return (String) workerIn.readObject();

            } catch (Exception e2) {
                return "ERROR: Both Primary (" + primaryIndex + ") and Replica (" + replicaIndex + ") Workers are offline!";
            }
        }
    }

    private void handleRateCommand(String command, ObjectOutputStream out) throws IOException {
        String gameName = command.split("\\|")[2];
        String response = sendToReplicaGroup(gameName, command);
        out.writeObject(response);
        out.flush();
    }

    private void handleRemoveCommand(String command, ObjectOutputStream out) throws IOException {
        String gameName = command.split("\\|")[2];
        String response = sendToReplicaGroup(gameName, "MANAGER_CMD|REMOVE|" + gameName);
        out.writeObject(response);
        out.flush();
    }

    private void handleEditRiskCommand(String command, ObjectOutputStream out) throws IOException {
        String[] parts = command.split("\\|");
        String gameName = parts[2];
        String newRisk = parts[3];
        String response = sendToReplicaGroup(gameName, "MANAGER_CMD|EDIT_RISK|" + gameName + "|" + newRisk);
        out.writeObject(response);
        out.flush();
    }

    private void handleBetCommand(String command, ObjectOutputStream out) throws IOException {
        String[] parts = command.split("\\|");
        String gameName = parts[2];
        double betAmount = Double.parseDouble(parts[3]);
        String playerName = parts[4];

        // 1. ΕΛΕΓΧΟΣ ΚΑΙ ΑΦΑΙΡΕΣΗ ΥΠΟΛΟΙΠΟΥ
        synchronized (Master.playerBalances) {
            double currentBalance = Master.playerBalances.getOrDefault(playerName, 0.0);
            if (currentBalance < betAmount) {
                out.writeObject("ERROR: Insufficient funds! Your balance is: " + currentBalance + " FUN");
                out.flush();
                return;
            }
            Master.playerBalances.put(playerName, currentBalance - betAmount);
        }

        // 2. Αποστολή στο Replica Group
        String workerCommand = "BET|" + gameName + "|" + betAmount + "|" + playerName;
        String response = sendToReplicaGroup(gameName, workerCommand);

        // 3. ΕΠΙΣΤΡΟΦΗ ΚΕΡΔΩΝ Η REFUND
        if (response.startsWith("PAYOUT|")) {
            String[] resParts = response.split("\\|");
            double wonAmount = Double.parseDouble(resParts[1]);
            String msg = resParts[2];

            synchronized (Master.playerBalances) {
                double currentBalance = Master.playerBalances.get(playerName);
                Master.playerBalances.put(playerName, currentBalance + wonAmount);
                out.writeObject(msg + "\n[Wallet Balance: " + (currentBalance + wonAmount) + " FUN]");
            }
        } else {
            // Είτε έγινε λάθος είτε είναι και οι 2 workers down -> Refund
            synchronized (Master.playerBalances) {
                double currentBalance = Master.playerBalances.get(playerName);
                Master.playerBalances.put(playerName, currentBalance + betAmount);
            }
            out.writeObject(response.replace("ERROR|", "ERROR: ") + "\n[Bet Refunded]");
        }
        out.flush();
    }

    private boolean sendToWorkerGeneric(Serializable obj, WorkerInfo worker) {
        try (Socket s = new Socket(worker.getIp(), worker.getPort());
             ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(s.getInputStream())) {
            out.writeObject(obj);
            out.flush();
            Object response = in.readObject();
            return response != null;
        } catch (Exception e) {
            return false;
        }
    }

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