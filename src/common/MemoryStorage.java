package common;

import java.util.Map;
import java.util.HashMap;

public class MemoryStorage {

    private final Map<String, Game> games = new HashMap<>();    // keeps games sth kyria mnhmh


    // prosthetei game me synchronization
    public synchronized void addGame(Game game) {

        games.put(game.getGameName(), game);
        System.out.println("[STORAGE] Added the game: " + game.getGameName());
    }


    // epistrefei ola ta games
    public synchronized Map<String, Game> getAllGames() {

        return new HashMap<>(games);    // epistrefei antigrafo gia apofygh ConcurrentModificationException an kapoio allo thread prosthesei to game
    }

    // game retrieval
    public synchronized Game getGame(String gameName) {
        return games.get(gameName);
    }

}
