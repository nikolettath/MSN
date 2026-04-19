package worker;

import common.Game;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PlayRequestHandler {

    public static String processBet(Game game, double betAmount) {
        try {
            // Aithma lipsis tyxaiou arithmou apo ton SRG Server
            try (Socket srgSocket = new Socket("localhost", 9090)) {
                ObjectOutputStream srgOut = new ObjectOutputStream(srgSocket.getOutputStream());
                srgOut.flush();
                ObjectInputStream srgIn = new ObjectInputStream(srgSocket.getInputStream());

                srgOut.writeUTF("REQUEST," + game.getGameName());
                srgOut.flush();

                String srgResponse = srgIn.readUTF();

                if (srgResponse.equals("GAME_NOT_FOUND")) {
                    return "ERROR: Game not registered in SRG";
                }

                // Exagogi dedomenwn apo tin apantisi
                String[] parts = srgResponse.split(",");
                int generatedNumber = Integer.parseInt(parts[0]);
                String receivedHash = parts[1];

                // Epalitheysi asfaleias meso SHA-256
                String myHash = generateHash(generatedNumber + game.getHashKey());
                if (!myHash.equals(receivedHash)) {
                    return "ERROR: Hash validation failed! Data corrupted or spoofed.";
                }

                // Ypologismos kerdous i zimias
                double payout = 0.0;
                String resultMessage;

                if (generatedNumber % 100 == 0) {
                    payout = betAmount * game.getJackpot();
                    resultMessage = "JACKPOT! You won " + payout + " FUN!";
                } else {
                    int index = generatedNumber % 10;
                    double multiplier = game.getRiskArray()[index];
                    payout = betAmount * multiplier;
                    if (multiplier == 0.0) {
                        resultMessage = "You lost. Payout: 0 FUN.";
                    } else {
                        resultMessage = "You won! Payout: " + payout + " FUN (Multiplier: " + multiplier + "x).";
                    }
                }

                // Asfalis enimerwsi statistikwn tou paixnidiou (Βάζουμε "Player1" προσωρινά ως όνομα)
                game.addBet("Player1", betAmount, payout);

                return resultMessage;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: Internal Worker Error";
        }
    }

    // Ypologismos SHA-256 hash
    private static String generateHash(String input) {
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