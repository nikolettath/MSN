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

            // reading aithma apo ton master
            Object request = input.readObject();

            // ------saving new game--------

            if (request instanceof Game)
            {
                Game game = (Game) request;
                storage.addGame(game);

                System.out.println("[WORKER] Saved game: " + game.getGameName());

                output.writeObject("SUCCESS: Game saved successfully.");    // confirmation to master
                output.flush();
            }

            // ---------game filtering (map)-----------

            else if (request instanceof FilterRequest)
            {
                FilterRequest filter = (FilterRequest) request;
                System.out.println("[WORKER] Starting mapping for filters.");

                List<Game> result = new ArrayList<>();
                Map<String, Game> localGames = storage.getAllGames(); // thread safe anagnwsh

                // diatrexei ta games sth memory storage
                for (Game g : localGames.values()) {
                    boolean matches = true;

                    // elegxos provider
                    if (filter.getProvider() != null && !filter.getProvider().isEmpty())
                    {
                        if (!filter.getProvider().equalsIgnoreCase(g.getProviderName())) {
                            matches = false;
                        }
                    }

                    // elegxos category ($ / $$ / $$$)
                    if (filter.getCategory() != null && !filter.getCategory().isEmpty())
                    {
                        if (!filter.getCategory().equalsIgnoreCase(g.getBetCategory())) {
                            matches = false;
                        }
                    }

                    // elegxos risk level (low / medium / high)
                    if (filter.getRiskLevel() != null && !filter.getRiskLevel().isEmpty())
                    {
                        if (!filter.getRiskLevel().equalsIgnoreCase(g.getRiskLevel())) {
                            matches = false;
                        }
                    }

                    if (matches) {
                        result.add(g);
                    }
                }

                sendToReducer(filter.getReducerHost(), filter.getReducerPort(), filter.getRequestId(), result);

                output.writeObject("MAP_COMPLETED");    // eidopoiei ton master oti o worker exei teleiwsei to map
                output.flush();
            }

            // ---------financial reports (map)---------

            else if (request instanceof ReportRequest)
            {
                ReportRequest reportReq = (ReportRequest) request;
                System.out.println("[WORKER] Starting estimation (map) for Report: " + reportReq.getReportType());

                Map<String, Double> partReport = new HashMap<>();
                Map<String, Game> localGames = storage.getAllGames(); // thread safe anagnwsh

                for (Game g : localGames.values())
                {
                    if ("BY_PROVIDER".equals(reportReq.getReportType())) {
                        String provider = g.getProviderName();
                        double profit = g.getCasinoProfit();
                        partReport.put(provider, partReport.getOrDefault(provider, 0.0) + profit);
                    }
                    else if ("BY_PLAYER".equals(reportReq.getReportType())) {
                        Map<String, Double> gamePlayerProfits = g.getCasinoProfitByPlayer();

                        // diatrexei olous tous players kai prosthetei ta values sto Worker's total partReport
                        for (Map.Entry<String, Double> entry : gamePlayerProfits.entrySet())
                        {
                            String player = entry.getKey();
                            double profitFromThisPlayer = entry.getValue();

                            // prosthetei Worker's partial report
                            partReport.put(player, partReport.getOrDefault(player, 0.0) + profitFromThisPlayer);
                        }
                    }
                }

                sendToReducer(reportReq.getReducerHost(), reportReq.getReducerPort(), reportReq.getRequestId(), partReport);

                output.writeObject("MAP_REPORT_COMPLETED");     // enhmerwnei ton master oti o worker teleiwse
                output.flush();
            }

            // --------- bet request ---------

            else if (request instanceof String)
            {
                String reqStr = (String) request;

                if (reqStr.startsWith("BET|")) {
                    String[] parts = reqStr.split("\\|");
                    String gameName = parts[1];
                    double betAmount = Double.parseDouble(parts[2]);

                    Game game = storage.getGame(gameName);

                    if (game == null) {
                        output.writeObject("ERROR: Game not found on this Worker!");
                    } else {
                        String result = PlayRequestHandler.processBet(game, betAmount);
                        output.writeObject(result);
                    }
                    output.flush();
                }
            }

            // --------unknown request----------

            else {
                System.out.println("[WORKER] Unknown request type.");
                output.writeObject("ERROR: Unknown request.");
                output.flush();
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[WORKER] Error connecting to WorkerThread: " + e.getMessage());
            e.printStackTrace();

        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("[WORKER] Error while closing the socket: " + e.getMessage());
            }
        }
    }


    private String hashSHA256(String input) {
        try
        {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");    // dhmiourgia tou digest gia ton algorithmo SHA-256
            byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));      // metatroph tou input se byte array kai ypologismos tou hash
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes)
            {
                String hex = Integer.toHexString(0xff & b);       // metatroph kathe byte se hexadecimal morfh
                if (hex.length() == 1) hexString.append('0');       // prosthhkh midenikou an to hex einai monopsifios gia na diathrithei to swsto mhkos
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error during SHA-256 calculation", e);
        }
    }


    private void sendToReducer(String host, int port, String requestId, Object dataToSend) {
        try (Socket reducerSocket = new Socket(host, port);
             ObjectOutputStream reducerOut = new ObjectOutputStream(reducerSocket.getOutputStream()))
        {
            // o Worker paketarei to ID mazi me ta dedomena
            reducerOut.writeObject(new Object[]{requestId, dataToSend});
            reducerOut.flush();
            System.out.println("[WORKER] Data successfully sent to Reducer (" + host + ":" + port + ").");

        } catch (IOException e) {
            System.err.println("[WORKER] Failed connection with Reducer: " + e.getMessage());
        }
    }
}