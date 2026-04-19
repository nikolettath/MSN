package common;

import common.Game;

import java.util.Map;
import java.util.HashMap;

public class MemoryStorage {

    private final Map<String, Game> games = new HashMap<>();    // keeps games in main memory


    // adding game with synchronization
    public synchronized void addGame(Game game) {

        games.put(game.getGameName(), game);
        System.out.println("[STORAGE] Added the game: " + game.getGameName());
    }


    // game retrieval
    public synchronized Game getGame(String gameName) {

        return games.get(gameName);
    }


    // returns all games
    public synchronized Map<String, Game> getAllGames() {

        return new HashMap<>(games);    //returns copy to avoid ConcurrentModificationException if another thread adds the game
    }
}
