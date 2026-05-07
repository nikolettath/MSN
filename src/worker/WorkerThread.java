package worker;

import common.Game;
import common.MemoryStorage;
import request.FilterRequest;
import request.ReportRequest;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkerThread extends Thread {

    private final Socket socket;
    private final MemoryStorage storage;

    public WorkerThread(Socket socket, MemoryStorage storage) {
        this.socket = socket;
        this.storage = storage;
    }

    @Override
    public void run() {
        try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {

            output.flush();
            Object request = input.readObject();

            // ==========================================
            // 1. ΑΠΟΘΗΚΕΥΣΗ ΝΕΟΥ ΠΑΙΧΝΙΔΙΟΥ
            // ==========================================
            if (request instanceof Game) {
                Game game = (Game) request;
                storage.addGame(game);
                System.out.println("[WORKER] Saved game: " + game.getGameName());
                output.writeObject("SUCCESS: Game saved successfully.");
                output.flush();
            }

            // ==========================================
            // 2. ΦΙΛΤΡΑΡΙΣΜΑ ΠΑΙΧΝΙΔΙΩΝ (MAP PHASE)
            // ==========================================
            else if (request instanceof FilterRequest) {
                FilterRequest filter = (FilterRequest) request;
                List<Game> result = new ArrayList<>();
                Map<String, Game> localGames = storage.getAllGames();

                for (Game g : localGames.values()) {
                    // Αγνοούμε τα παιχνίδια που έχουν διαγραφεί από τον Manager
                    if (!g.isActive()) continue;

                    boolean matches = true;

                    if (filter.getProvider() != null && !filter.getProvider().isEmpty() && !filter.getProvider().equals("ANY")) {
                        if (!filter.getProvider().equalsIgnoreCase(g.getProviderName())) matches = false;
                    }
                    if (filter.getCategory() != null && !filter.getCategory().isEmpty() && !filter.getCategory().equals("ANY")) {
                        if (!filter.getCategory().equalsIgnoreCase(g.getBetCategory())) matches = false;
                    }
                    if (filter.getRiskLevel() != null && !filter.getRiskLevel().isEmpty() && !filter.getRiskLevel().equals("ANY")) {
                        if (!filter.getRiskLevel().equalsIgnoreCase(g.getRiskLevel())) matches = false;
                    }
                    // Φιλτράρισμα βάσει ελάχιστων αστεριών
                    if (g.getStars() < filter.getMinStars()) {
                        matches = false;
                    }

                    if (matches) result.add(g);
                }

                sendToReducer(filter.getReducerHost(), filter.getReducerPort(), filter.getRequestId(), result);
                output.writeObject("MAP_COMPLETED");
                output.flush();
            }

            // ==========================================
            // 3. ΟΙΚΟΝΟΜΙΚΑ REPORTS (MAP PHASE)
            // ==========================================
            else if (request instanceof ReportRequest) {
                ReportRequest reportReq = (ReportRequest) request;
                Map<String, Double> partReport = new HashMap<>();
                Map<String, Game> localGames = storage.getAllGames();

                for (Game g : localGames.values()) {
                    if ("BY_PROVIDER".equals(reportReq.getReportType())) {
                        String provider = g.getProviderName();
                        partReport.put(provider, partReport.getOrDefault(provider, 0.0) + g.getCasinoProfit());
                    }
                    else if ("BY_PLAYER".equals(reportReq.getReportType())) {
                        Map<String, Double> gamePlayerProfits = g.getCasinoProfitByPlayer();
                        for (Map.Entry<String, Double> entry : gamePlayerProfits.entrySet()) {
                            partReport.put(entry.getKey(), partReport.getOrDefault(entry.getKey(), 0.0) + entry.getValue());
                        }
                    }
                }
                sendToReducer(reportReq.getReducerHost(), reportReq.getReducerPort(), reportReq.getRequestId(), partReport);
                output.writeObject("MAP_REPORT_COMPLETED");
                output.flush();
            }

            // ==========================================
            // 4. ΕΝΤΟΛΕΣ ΚΕΙΜΕΝΟΥ (BET, RATE, REMOVE, EDIT)
            // ==========================================
            else if (request instanceof String) {
                String reqStr = (String) request;

                // --- ΕΝΤΟΛΗ ΣΤΟΙΧΗΜΑΤΟΣ ---
                if (reqStr.startsWith("BET|")) {
                    String[] parts = reqStr.split("\\|");
                    String gameName = parts[1];
                    double betAmount = Double.parseDouble(parts[2]);
                    String playerName = parts[3];

                    Game game = storage.getGame(gameName);

                    if (game == null || !game.isActive()) {
                        output.writeObject("ERROR|Game not found or inactive.");
                    } else {
                        // Καλούμε τον SRG και παίρνουμε το αποτέλεσμα
                        String result = PlayRequestHandler.processBet(game, betAmount, playerName);
                        output.writeObject("PAYOUT|" + result);
                    }
                    output.flush();
                }
                // --- ΕΝΤΟΛΗ ΒΑΘΜΟΛΟΓΙΑΣ ---
                else if (reqStr.startsWith("PLAYER_CMD|RATE|")) {
                    String[] parts = reqStr.split("\\|");
                    String gameName = parts[2];
                    int stars = Integer.parseInt(parts[3]);

                    Game game = storage.getGame(gameName);
                    if (game != null && game.isActive()) {
                        game.addRating(stars);
                        output.writeObject("SUCCESS: You rated " + gameName + " with " + stars + " stars!");
                    } else {
                        output.writeObject("ERROR: Game not found or inactive.");
                    }
                    output.flush();
                }
                // --- ΕΝΤΟΛΗ ΑΦΑΙΡΕΣΗΣ ΠΑΙΧΝΙΔΙΟΥ ---
                else if (reqStr.startsWith("MANAGER_CMD|REMOVE|")) {
                    String gameName = reqStr.split("\\|")[2];
                    Game game = storage.getGame(gameName);
                    if (game != null) {
                        game.setActive(false); // Soft Delete
                        output.writeObject("SUCCESS: Game '" + gameName + "' removed.");
                    } else {
                        output.writeObject("ERROR: Game not found on this worker.");
                    }
                    output.flush();
                }
                // --- ΕΝΤΟΛΗ ΑΛΛΑΓΗΣ ΡΙΣΚΟΥ ---
                else if (reqStr.startsWith("MANAGER_CMD|EDIT_RISK|")) {
                    String[] parts = reqStr.split("\\|");
                    String gameName = parts[2];
                    String newRisk = parts[3];
                    Game game = storage.getGame(gameName);
                    if (game != null) {
                        game.setRiskLevel(newRisk);
                        output.writeObject("SUCCESS: Risk level updated to '" + newRisk + "'.");
                    } else {
                        output.writeObject("ERROR: Game not found on this worker.");
                    }
                    output.flush();
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException e) {}
        }
    }

    // Βοηθητική μέθοδος για αποστολή δεδομένων στον Reducer
    private void sendToReducer(String host, int port, String requestId, Object dataToSend) {
        try (Socket reducerSocket = new Socket(host, port);
             ObjectOutputStream reducerOut = new ObjectOutputStream(reducerSocket.getOutputStream())) {
            reducerOut.writeObject(new Object[]{requestId, dataToSend});
            reducerOut.flush();
        } catch (IOException e) {
            System.err.println("[WORKER] Failed connection with Reducer: " + e.getMessage());
        }
    }
}