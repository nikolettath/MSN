package engine;

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
        try
        {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            String request = (String) ois.readObject();

            // elegxos aithmatos (diaforetiko format master/worker)
            if (request.startsWith("REGISTER")) {
                String[] tokens = request.split(",");
                String gameName = tokens[1];
                String hashKey = tokens[2];

                synchronized (gameGenerators) {
                    if (!gameGenerators.containsKey(gameName))
                    {
                        GameRandomGenerator generator = new GameRandomGenerator(gameName, hashKey);
                        // ksekinhma tou producer thread
                        generator.start();
                        gameGenerators.put(gameName, generator);

                        oos.writeObject("OK");
                    } else {
                        // apanthsh OK estw ki an yparxei hdh
                        oos.writeObject("OK");
                    }
                }
            }
            else if (request.startsWith("GET_NUMBER"))
            {
                // aithma worker me format GET_NUMBER|gameName
                String[] tokens = request.split("\\|");
                String gameName = tokens[1];

                GameRandomGenerator generator;
                synchronized (gameGenerators) {
                    generator = gameGenerators.get(gameName);
                }

                if (generator != null)
                {
                    String response = generator.getNextNumberWithHash();
                    oos.writeObject(response);
                } else
                {
                    oos.writeObject("GAME_NOT_FOUND");
                }
            }

            oos.flush();
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}