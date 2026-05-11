package engine;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SRGServer {

    // map gia antistoixish kathe paixnidiou me th dikh tou genhtria
    private static final Map<String, GameRandomGenerator> gameGenerators = new HashMap<>();

    public static void main(String[] args) {

        int port = 9090;    // porta tou SRG Server

        try (ServerSocket serverSocket = new ServerSocket(port))
        {
            System.out.println("SRG Server started at port " + port);

            // loop pou asygxrona apodexetai nees syndeseis (workers h master)
            while (true)
            {
                Socket clientSocket = serverSocket.accept();
                // dhmiourgia neou thread gia na diaxeiristei to aithma xwris na mplokarei o server
                new SRGClientHandler(clientSocket, gameGenerators).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}