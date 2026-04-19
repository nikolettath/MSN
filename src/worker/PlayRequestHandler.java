package worker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import common.Game;

public class PlayRequestHandler extends Thread {

    private final Game game;
    private final double betAmount;
    private final Socket masterSocket; // Socket epikoinwnias me ton Master

    public PlayRequestHandler(Socket masterSocket, Game game, double betAmount) {
        this.masterSocket = masterSocket;
        this.game = game;
        this.betAmount = betAmount;
    }

    @Override
    public void run() {
        try {
            // Aithma lipsis tyxaiou arithmou apo ton SRG Server
            Socket srgSocket = new Socket("localhost", 9090);
            ObjectOutputStream srgOut = new ObjectOutputStream(srgSocket.getOutputStream());
            out.flush();
            ObjectInputStream srgIn = new ObjectInputStream(srgSocket.getInputStream());

            srgOut.writeUTF("REQUEST," + game.getGameName());
            srgOut.flush();

            String srgResponse = srgIn.readUTF();
            // Kleisimo tis syndesis me ton SRG meta tin lipsi twn dedomenwn
            srgSocket.close();

            if (srgResponse.equals("GAME_NOT_FOUND")) {
                sendResultToMaster("ERROR: Game not registered in SRG");
                return;
            }

            // Exagogi dedomenwn apo tin apantisi
            String[] parts = srgResponse.split(",");
            int generatedNumber = Integer.parseInt(parts[0]);
            String receivedHash = parts[1];

            // Epalitheysi asfaleias meso SHA-256
            String myHash = generateHash(generatedNumber + game.getHashKey());
            if (!myHash.equals(receivedHash)) {
                // Termatismos pontarismatos se periptwsi apotyxias epalitheysis
                sendResultToMaster("ERROR: Hash validation failed! Data corrupted or spoofed.");
                return;
            }

            // Ypologismos kerdous i zimias
            double payout = 0.0;
            String resultMessage;

            if (generatedNumber % 100 == 0) {
                // Periptwsi Jackpot
                payout = betAmount * game.getJackpot();
                resultMessage = "JACKPOT! You won " + payout + " FUN!";
            } else {
                // Kanoniko pontarisma sumfwna me ton pinaka riskou
                int index = generatedNumber % 10;
                double multiplier = game.getRiskArray()[index];
                payout = betAmount * multiplier;
                if (multiplier == 0.0) {
                    resultMessage = "You lost. Payout: 0 FUN.";
                } else {
                    resultMessage = "You won! Payout: " + payout + " FUN (Multiplier: " + multiplier + "x).";
                }
            }

            // Asfalis enimerwsi statistikwn tou paixnidiou
            game.updateSystemProfit(betAmount, payout);

            // Epistrofi apotelesmatos ston Master
            sendResultToMaster(resultMessage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendResultToMaster(String message) {
        try {
            ObjectOutputStream masterOut = new ObjectOutputStream(masterSocket.getOutputStream());
            masterOut.writeUTF(message);
            masterOut.flush();
            masterSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Ypologismos SHA-256 hash
    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}