package worker;

import common.Game;
import common.MemoryStorage;
import request.FilterRequest;
import request.ReportRequest;
import java.io.*;
import java.net.Socket;
import java.util.*;

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

            if (request instanceof Game) {
                storage.addGame((Game) request);
                output.writeObject("SUCCESS: Game saved successfully.");
                output.flush();
            }
            else if (request instanceof FilterRequest) {
                FilterRequest filter = (FilterRequest) request;
                List<Game> result = new ArrayList<>();
                for (Game g : storage.getAllGames().values()) {
                    if (!g.isActive()) continue;
                    boolean matches = true;
                    if (filter.getProvider() != null && !filter.getProvider().equals("ANY") && !filter.getProvider().equalsIgnoreCase(g.getProviderName())) matches = false;
                    if (filter.getCategory() != null && !filter.getCategory().equals("ANY") && !filter.getCategory().equalsIgnoreCase(g.getBetCategory())) matches = false;
                    if (filter.getRiskLevel() != null && !filter.getRiskLevel().equals("ANY") && !filter.getRiskLevel().equalsIgnoreCase(g.getRiskLevel())) matches = false;
                    if (g.getStars() < filter.getMinStars()) matches = false;

                    if (matches) result.add(g);
                }
                sendToReducer(filter.getReducerHost(), filter.getReducerPort(), filter.getRequestId(), result);
                output.writeObject("MAP_COMPLETED");
                output.flush();
            }
            else if (request instanceof ReportRequest) {
                ReportRequest reportReq = (ReportRequest) request;
                Map<String, Double> partReport = new HashMap<>();
                for (Game g : storage.getAllGames().values()) {
                    if ("BY_PROVIDER".equals(reportReq.getReportType())) {
                        partReport.put(g.getProviderName(), partReport.getOrDefault(g.getProviderName(), 0.0) + g.getCasinoProfit());
                    } else if ("BY_PLAYER".equals(reportReq.getReportType())) {
                        for (Map.Entry<String, Double> entry : g.getCasinoProfitByPlayer().entrySet()) {
                            partReport.put(entry.getKey(), partReport.getOrDefault(entry.getKey(), 0.0) + entry.getValue());
                        }
                    }
                }
                sendToReducer(reportReq.getReducerHost(), reportReq.getReducerPort(), reportReq.getRequestId(), partReport);
                output.writeObject("MAP_REPORT_COMPLETED");
                output.flush();
            }
            else if (request instanceof String) {
                String reqStr = (String) request;

                if (reqStr.startsWith("BET|")) {
                    String[] parts = reqStr.split("\\|");
                    Game game = storage.getGame(parts[1]);
                    if (game == null || !game.isActive()) output.writeObject("ERROR|Game not found or inactive.");
                    else output.writeObject("PAYOUT|" + PlayRequestHandler.processBet(game, Double.parseDouble(parts[2]), parts[3]));
                    output.flush();
                }
                else if (reqStr.startsWith("PLAYER_CMD|RATE|")) {
                    String[] parts = reqStr.split("\\|");
                    Game game = storage.getGame(parts[2]);
                    if (game != null && game.isActive()) {
                        game.addRating(Integer.parseInt(parts[3]));
                        output.writeObject("SUCCESS: Rated " + parts[2] + " with " + parts[3] + " stars.");
                    } else output.writeObject("ERROR|Game not found.");
                    output.flush();
                }
                else if (reqStr.startsWith("MANAGER_CMD|REMOVE|")) {
                    Game game = storage.getGame(reqStr.split("\\|")[2]);
                    if (game != null) { game.setActive(false); output.writeObject("SUCCESS: Game removed."); }
                    else output.writeObject("ERROR|Game not found.");
                    output.flush();
                }
                else if (reqStr.startsWith("MANAGER_CMD|RESTORE|")) {
                    Game game = storage.getGame(reqStr.split("\\|")[2]);
                    if (game != null) { game.setActive(true); output.writeObject("SUCCESS: Game restored."); }
                    else output.writeObject("ERROR|Game not found.");
                    output.flush();
                }
                else if (reqStr.startsWith("MANAGER_CMD|EDIT_RISK|")) {
                    String[] parts = reqStr.split("\\|");
                    Game game = storage.getGame(parts[2]);
                    if (game != null) { game.setRiskLevel(parts[3]); output.writeObject("SUCCESS: Risk updated."); }
                    else output.writeObject("ERROR|Game not found.");
                    output.flush();
                }
                else if (reqStr.startsWith("MANAGER_CMD|EDIT_LIMITS|")) {
                    String[] parts = reqStr.split("\\|");
                    Game game = storage.getGame(parts[2]);
                    if (game != null) {
                        game.setMinBet(Double.parseDouble(parts[3])); game.setMaxBet(Double.parseDouble(parts[4]));
                        output.writeObject("SUCCESS: Limits updated. Category is now: " + game.getBetCategory());
                    } else output.writeObject("ERROR|Game not found.");
                    output.flush();
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        finally { try { if (socket != null) socket.close(); } catch (IOException e) {} }
    }

    private void sendToReducer(String host, int port, String id, Object data) {
        try (Socket s = new Socket(host, port); ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream())) {
            out.writeObject(new Object[]{id, data}); out.flush();
        } catch (IOException e) { System.err.println("Reducer error: " + e.getMessage()); }
    }
}