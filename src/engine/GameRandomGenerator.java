package engine;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class GameRandomGenerator extends Thread {

    private final String gameName;
    // to secret S tou paixnidiou
    private final String hashKey;
    private final Queue<Integer> buffer;
    // megisto megethos tou buffer
    private final int MAX_CAPACITY = 10;
    private final Random random;

    public GameRandomGenerator(String gameName, String hashKey) {
        this.gameName = gameName;
        this.hashKey = hashKey;
        this.buffer = new LinkedList<>();
        this.random = new Random();
    }


    // producer: paragei tyxaious arithmous kai gemizei to buffer
    @Override
    public void run() {
        while (true)
        {
            synchronized (buffer) {
                // anamonh otan to buffer einai gemato
                while (buffer.size() == MAX_CAPACITY)
                {
                    try {
                        buffer.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // paragogh tyxaiou arithmou [0, 999]
                int nextNumber = random.nextInt(1000);
                buffer.add(nextNumber);

                // eidopoihsh stous consumers oti prostethike arithmos
                buffer.notifyAll();
            }

            // mikrh kathysterhsh gia apofygh yperfortwshs CPU
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    // consumer: exagogh arithmou kai dhmiourgia sha256 hash
    public String getNextNumberWithHash() {
        int number;
        synchronized (buffer) {
            // anamonh otan to buffer einai adeio
            while (buffer.isEmpty())
            {
                try {
                    buffer.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // afairesh tou prwtou stoixeiou apo thn oura
            number = buffer.poll();

            // eidopoihsh ston producer oti yparxei diathesimos xwros
            buffer.notifyAll();
        }

        // dhmiourgia tou sha256 (arithmos + secret)
        String hash = generateHash(number + hashKey);

        // epistrofh apotelesmatos sth morfh: number|hash
        return number + "|" + hash;
    }


    // metatroph tou input se sha256 hash
    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes());
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    // vohthitikh methodos gia metatroph byte array se hex string
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