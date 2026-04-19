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

            // elenxoume ti hrthe prin kanoume split giat o Master stelnei me komma kai o Worker me katheto
            if (request.startsWith("REGISTER")) {
                String[] tokens = request.split(",");
                String gameName = tokens[1];
                String hashKey = tokens[2];

                synchronized (gameGenerators) {
                    if (!gameGenerators.containsKey(gameName))
                    {
                        GameRandomGenerator generator = new GameRandomGenerator(gameName, hashKey);
                        generator.start();          // Ksekinaei to thread tou producer
                        gameGenerators.put(gameName, generator);

                        oos.writeObject("OK");
                    } else {
                        oos.writeObject("OK");      // Apantame OK akoma kai an uparxei hdh gia na mhn kollhsei o Master
                    }
                }
            }
            else if (request.startsWith("GET_NUMBER"))
            {
                // o worker stelnei GET_NUMBER|gameName
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