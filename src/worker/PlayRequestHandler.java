package worker;

import common.Game;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class PlayRequestHandler {

    public static String processBet(Game game, double betAmount) {
        try
        {
            // aithma lhpshs tyxaiou arithmou apo ton SRG Server
            try (Socket srgSocket = new Socket("localhost", 9090))
            {
                ObjectOutputStream srgOut = new ObjectOutputStream(srgSocket.getOutputStream());
                srgOut.flush();
                ObjectInputStream srgIn = new ObjectInputStream(srgSocket.getInputStream());

                srgOut.writeObject("GET_NUMBER|" + game.getGameName());
                srgOut.flush();

                String srgResponse = (String) srgIn.readObject();

                if (srgResponse.equals("GAME_NOT_FOUND")) {
                    return "ERROR: Game not registered in SRG";
                }

                // exagwgh dedomenwn apo thn apanthsh
                String[] parts = srgResponse.split("\\|");
                int generatedNumber = Integer.parseInt(parts[0]);
                String receivedHash = parts[1];

                // epalhtheysh asfaleias meso SHA-256
                String myHash = generateHash(generatedNumber + game.getHashKey());
                if (!myHash.equals(receivedHash)) {
                    return "ERROR: Hash validation failed! Data corrupted or spoofed.";
                }

                double payout = 0.0;        // ypologismos kerdous h' zhmias
                String resultMessage;

                if (generatedNumber % 100 == 0)
                {
                    payout = betAmount * game.getJackpot();
                    resultMessage = "JACKPOT! You won " + payout + " FUN!";
                } else
                {
                    int index = generatedNumber % 10;
                    double multiplier = game.getRiskArray()[index];
                    payout = betAmount * multiplier;
                    if (multiplier == 0.0)
                    {
                        resultMessage = "You lost. Payout: 0 FUN.";
                    } else
                    {
                        resultMessage = "You won! Payout: " + payout + " FUN (Multiplier: " + multiplier + "x).";
                    }
                }

                // asfalhs enhmerwsh statistikwn tou game - Player1 ws proswrino onoma
                game.addBet("Player1", betAmount, payout);

                return resultMessage;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: Internal Worker Error";
        }
    }


    // ypologismos SHA-256 hash
    private static String generateHash(String input) {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash)
            {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                {
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