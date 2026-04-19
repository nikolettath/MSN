package engine;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

public class SRGClientHandler extends Thread {

    private final Socket socket;
    private final Map<String, GameRandomGenerator> gameGenerators;

    public SRGClientHandler(Socket socket, Map<String, GameRandomGenerator> gameGenerators) {
        this.socket = socket;
        this.gameGenerators = gameGenerators;
    }


    @Override
    public void run() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            // 1. ΑΛΛΑΓΗ: Διαβάζουμε Object αντί για UTF
            String request = (String) ois.readObject();

            // 2. ΑΛΛΑΓΗ: Ελέγχουμε τι ήρθε πριν κάνουμε split, γιατί
            // ο Master στέλνει με κόμμα (,) και ο Worker με κάθετο (|)
            if (request.startsWith("REGISTER")) {
                String[] tokens = request.split(",");
                String gameName = tokens[1];
                String hashKey = tokens[2];

                synchronized (gameGenerators) {
                    if (!gameGenerators.containsKey(gameName)) {
                        GameRandomGenerator generator = new GameRandomGenerator(gameName, hashKey);
                        generator.start(); // Ξεκινάει το Thread του Producer
                        gameGenerators.put(gameName, generator);

                        // 3. ΑΛΛΑΓΗ: Ο Master περιμένει να ακούσει σκέτο "OK" (με writeObject)
                        oos.writeObject("OK");
                    } else {
                        oos.writeObject("OK"); // Απαντάμε ΟΚ ακόμα και αν υπάρχει ήδη για να μην κολλήσει ο Master
                    }
                }
            }
            else if (request.startsWith("GET_NUMBER")) {
                // Ο Worker στέλνει: GET_NUMBER|gameName
                String[] tokens = request.split("\\|");
                String gameName = tokens[1];

                GameRandomGenerator generator;
                synchronized (gameGenerators) {
                    generator = gameGenerators.get(gameName);
                }

                if (generator != null) {
                    String response = generator.getNextNumberWithHash();
                    oos.writeObject(response); // ΑΛΛΑΓΗ: writeObject
                } else {
                    oos.writeObject("GAME_NOT_FOUND"); // ΑΛΛΑΓΗ: writeObject
                }
            }

            oos.flush();
            socket.close();

        } catch (Exception e) { // Catch γενικό Exception για να πιάσει και το ClassNotFoundException
            e.printStackTrace();
        }
    }
}