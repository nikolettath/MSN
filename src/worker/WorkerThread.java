package worker;

import common.Game;
import common.MemoryStorage;
import request.FilterRequest;
import request.ReportRequest;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream()))
        {
            // reading request from master
            Object request = input.readObject();

            // ------saving new game--------
            if (request instanceof Game)
            {
                Game game = (Game) request;
                storage.addGame(game);

                System.out.println("[WORKER] Saved game: " + game.getGameName());

                output.writeObject("SUCCESS: Game saved successfully."); // confirmation to master
                output.flush();
            }

            // ---------game filtering (map)-----------
            else if (request instanceof FilterRequest)
            {
                FilterRequest filter = (FilterRequest) request;
                System.out.println("[WORKER] Starting mapping for filters.");

                List<Game> result = new ArrayList<>();
                Map<String, Game> localGames = storage.getAllGames(); // safe for threads to read

                // traversing games in memory storage
                for (Game g : localGames.values()) {
                    boolean matches = true;

                    // provider check (Χρήση getProviderName() από την κλάση Game)
                    if (filter.getProvider() != null && !filter.getProvider().isEmpty()) {
                        if (!filter.getProvider().equalsIgnoreCase(g.getProviderName())) {
                            matches = false;
                        }
                    }

                    // category check (Χρήση getBetCategory() από την κλάση Game)
                    if (filter.getCategory() != null && !filter.getCategory().isEmpty()) {
                        if (!filter.getCategory().equalsIgnoreCase(g.getBetCategory())) {
                            matches = false;
                        }
                    }

                    if (matches) {
                        result.add(g);
                    }
                }

                sendToReducer(filter.getReducerHost(), filter.getReducerPort(), result); // sending results to reducer

                output.writeObject("MAP_COMPLETED"); // notifying master that worker has finished its map
                output.flush();
            }

            // ---------financial reports (map)---------
            else if (request instanceof ReportRequest)
            {
                ReportRequest reportReq = (ReportRequest) request;
                System.out.println("[WORKER] Starting estimation (map) for Report: " + reportReq.getReportType());

                Map<String, Double> partReport = new HashMap<>();
                Map<String, Game> localGames = storage.getAllGames(); // safe for threads to read

                for (Game g : localGames.values())
                {
                    if ("BY_PROVIDER".equals(reportReq.getReportType()))
                    {
                        String provider = g.getProviderName();      // using getProviderName()
                        double profit = g.getCasinoProfit();

                        // aggregation and part of sum up
                        partReport.put(provider, partReport.getOrDefault(provider, 0.0) + profit);
                    }
                    // [Μέσα στο WorkerThread, στο reportReq instanceof ReportRequest]

                    if ("BY_PROVIDER".equals(reportReq.getReportType()))
                    {
                        String provider = g.getProviderName();
                        double profit = g.getCasinoProfit();
                        partReport.put(provider, partReport.getOrDefault(provider, 0.0) + profit);
                    }
                    else if ("BY_PLAYER".equals(reportReq.getReportType()))
                    {
                        Map<String, Double> gamePlayerProfits = g.getCasinoProfitByPlayer();

                        // traversing all players and adding up values to Worker's total partReport
                        for (Map.Entry<String, Double> entry : gamePlayerProfits.entrySet())
                        {
                            String player = entry.getKey();
                            double profitFromThisPlayer = entry.getValue();

                            // add up Worker's partial report
                            partReport.put(player, partReport.getOrDefault(player, 0.0) + profitFromThisPlayer);
                        }
                    }
                }

                // sending HashMap with some of the summed up reports to reducer
                sendToReducer(reportReq.getReducerHost(), reportReq.getReducerPort(), partReport);

                output.writeObject("MAP_REPORT_COMPLETED"); // updating master that worker is finished
                output.flush();
            }

            // --------unknown request----------
            else
            {
                System.out.println("[WORKER] Unknown request type.");
                output.writeObject("ERROR: Unknown request.");
                output.flush();
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[WORKER] Error connecting to WorkerThread: " + e.getMessage());
            e.printStackTrace();

        } finally
        {
            // making sure Master's socket always closes
            try
            {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("[WORKER] Error while closing the socket: " + e.getMessage());
            }
        }
    }


     // helper function for sending data (list or map) to reducer through a new temporary TCP Socket
    private void sendToReducer(String host, int port, Object dataToSend) {

        try (Socket reducerSocket = new Socket(host, port);
                ObjectOutputStream reducerOut = new ObjectOutputStream(reducerSocket.getOutputStream()))
        {
            reducerOut.writeObject(dataToSend);
            reducerOut.flush();
            System.out.println("[WORKER] Data successfully sent to Reducer (" + host + ":" + port + ").");

        } catch (IOException e) {
            System.err.println("[WORKER] Failed connection with Reducer: " + e.getMessage());
        }
    }
}