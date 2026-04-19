package engine;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SRGServer {

    // Dimiourgia antistoixias paixnidiou kai genitrias tyxaiwn arithmwn
    private static final Map<String, GameRandomGenerator> gameGenerators = new HashMap<>();

    public static void main(String[] args) {

        int port = 9090; // Thira tou SRG Server

        try (ServerSocket serverSocket = new ServerSocket(port))
        {
            System.out.println("SRG Server started at port " + port);

            while (true)
            {
                Socket clientSocket = serverSocket.accept();
                new SRGClientHandler(clientSocket, gameGenerators).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
