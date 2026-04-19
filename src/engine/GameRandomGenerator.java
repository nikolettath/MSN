package engine;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class GameRandomGenerator extends Thread {

    private final String gameName;
    private final String hashKey; // To secret S tou paixnidiou
    private final Queue<Integer> buffer;
    private final int MAX_CAPACITY = 10; // Megisto megethos tou buffer
    private final Random random;

    public GameRandomGenerator(String gameName, String hashKey) {
        this.gameName = gameName;
        this.hashKey = hashKey;
        this.buffer = new LinkedList<>();
        this.random = new Random();
    }

    // Producer: Paragei tyxaious arithmous kai gemizei ton buffer
    @Override
    public void run() {
        while (true) {
            synchronized (buffer) {
                // Anamonh otan o buffer einai pliris
                while (buffer.size() == MAX_CAPACITY) {
                    try {
                        buffer.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Paragogi tyxaiou arithmou sto diastima [0, 999]
                int nextNumber = random.nextInt(1000);
                buffer.add(nextNumber);

                // Eidopoiisi twn threads pou vriskontai se anamonh
                buffer.notifyAll();
            }

            // Mikri kathysterisi gia beltistopoiisi porwn CPU
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Consumer: Exagogi epomenou arithmou kai dhmiourgia SHA-256 hash
    public String getNextNumberWithHash() {
        int number;
        synchronized (buffer) {
            // Anamonh otan o buffer einai adeios
            while (buffer.isEmpty()) {
                try {
                    buffer.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Exagogi stoixeiou apo tin oura
            number = buffer.poll();

            // Eidopoiisi tou producer oti apeutherothike xwros ston buffer
            buffer.notifyAll();
        }

        // Dimiourgia tou SHA-256 Hash
        String hash = generateHash(number + hashKey);

        // Epistrofi tou aithmou kai tou hash ws string me diaxwristiko to komma
        return number + "|" + hash;
    }

    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes());
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}