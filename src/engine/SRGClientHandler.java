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
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            // Anagnosi aitimatou apo ton client
            String request = ois.readUTF();
            String[] tokens = request.split(",");
            String action = tokens[0];
            String gameName = tokens[1];

            if (action.equals("REGISTER")) {
                String hashKey = tokens[2];
                // Sygxronismos gia asfali prosvasi kai eggrafi sto shared Map
                synchronized (gameGenerators) {
                    if (!gameGenerators.containsKey(gameName)) {
                        GameRandomGenerator generator = new GameRandomGenerator(gameName, hashKey);
                        generator.start(); // Ekkinaei to Thread tou Producer
                        gameGenerators.put(gameName, generator);
                        oos.writeUTF("REGISTERED_OK");
                    } else {
                        oos.writeUTF("ALREADY_REGISTERED");
                    }
                }
            } else if (action.equals("REQUEST")) {
                GameRandomGenerator generator;
                synchronized (gameGenerators) {
                    generator = gameGenerators.get(gameName);
                }

                if (generator != null) {
                    // Lhpsh tou arithmou kai tou hash apo ton Consumer
                    String response = generator.getNextNumberWithHash();
                    oos.writeUTF(response);
                } else {
                    oos.writeUTF("GAME_NOT_FOUND");
                }
            }

            oos.flush();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}