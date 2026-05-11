package worker;

import common.Game;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PlayRequestHandler {

    // ektelesh pontarismatos kai ypologismos kerdous
    public static String processBet(Game game, double betAmount, String playerName) {
        try {
<<<<<<< HEAD
            try (Socket srgSocket = new Socket(game.getSrgIp(), 9090)) {
=======
            // syndesh me ton SRG gia lhpsh tyxaiou arithmou
            try (Socket srgSocket = new Socket("localhost", 9090)) {
>>>>>>> a062b66e9299524251c8dc15012372704f05bcc0
                ObjectOutputStream srgOut = new ObjectOutputStream(srgSocket.getOutputStream());
                srgOut.flush();
                ObjectInputStream srgIn = new ObjectInputStream(srgSocket.getInputStream());

                srgOut.writeObject("GET_NUMBER|" + game.getGameName());
                srgOut.flush();

                String srgResponse = (String) srgIn.readObject();

                if (srgResponse.equals("GAME_NOT_FOUND")) {
                    return "-1|ERROR: Game not registered in SRG";
                }

                // diaxorismos arithmou kai hash gia epalitheush
                String[] parts = srgResponse.split("\\|");
                int generatedNumber = Integer.parseInt(parts[0]);
                String receivedHash = parts[1];

                // epalitheush hash me to secret key tou paixnidiou
                String myHash = generateHash(generatedNumber + game.getHashKey());
                if (!myHash.equals(receivedHash)) {
                    return "-1|ERROR: Hash validation failed! Data corrupted.";
                }

                double payout = 0.0;
                String resultMessage;

                // elegxos gia jackpot (arithmos mod 100 == 0)
                if (generatedNumber % 100 == 0) {
                    payout = betAmount * game.getJackpot();
                    resultMessage = "JACKPOT! You won " + payout + " FUN!";
                } else {
                    // ypologismos kerdous vash pinaka riskou
                    int index = generatedNumber % 10;
                    double multiplier = game.getRiskArray()[index];
                    payout = betAmount * multiplier;
                    if (multiplier == 0.0) {
                        resultMessage = "You lost. Payout: 0 FUN.";
                    } else {
                        resultMessage = "You won! Payout: " + payout + " FUN (Multiplier: " + multiplier + "x).";
                    }
                }

                // enhmerwsh statistikwn toy paixnidiou sth mnhmh
                game.addBet(playerName, betAmount, payout);

                // epistrofh apotelesmatos: payout|message
                return payout + "|" + resultMessage;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "-1|ERROR: Internal Worker Error";
        }
    }

    // methodos dhmiourgias sha-256 hash gia epalitheush dedomenwn
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