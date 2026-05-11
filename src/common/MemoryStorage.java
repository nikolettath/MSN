package common;

import java.util.Map;
import java.util.HashMap;

public class MemoryStorage {

    // domh dedomenwn gia apothhkeysh paixnidiwn sth mnhmh (RAM) tou worker
    private final Map<String, Game> games = new HashMap<>();

    // prosthkh h enhmerwsh paixnidiou me asfaleia
    public synchronized void addGame(Game game) {
        games.put(game.getGameName(), game);
        System.out.println("[STORAGE] Added the game: " + game.getGameName());
    }

    // epistrofh olwn twn paixnidiwn
    public synchronized Map<String, Game> getAllGames() {
        // epistrefei antigrafo gia na mhn ephreastei to map an allaksei kata th xrhsh tou
        return new HashMap<>(games);
    }

    // eyresh kai epistrofh sygkekrimenou paixnidiou mesw tou onomatos tou
    public synchronized Game getGame(String gameName) {
        return games.get(gameName);
    }
}