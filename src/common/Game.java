package common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Game implements Serializable {

    private static final long serialVersionUID = 1L;

    //pedia pou diavazontai apo to JSON
    private String gameName;
    private String providerName;
    private int stars;
    private int noOfVotes;
    private String gameLogo;
    private double minBet;
    private double maxBet;
    private String riskLevel;
    private String hashKey; // Secret S

    //pedia pou ypologizontai apo to systhma
    private String betCategory;
    private int jackpot;

    // statistics
    private double totalBets;
    private double totalPayouts;
    
    // gia katagrafh kerdous/zhmias kerdos ana paixth -- kleidi = onoma paixth, timh = kerdos kazino (Bets - Payouts)
    private final Map<String, Double> casinoProfitByPlayer;


    //pinakes riskou
    private static final double[] LOW_RISK = {0.0, 0.0, 0.0, 0.1, 0.5, 1.0, 1.1, 1.3, 2.0, 2.5};
    private static final double[] MEDIUM_RISK = {0.0, 0.0, 0.0, 0.0, 0.0, 0.5, 1.0, 1.5, 2.5, 3.5};
    private static final double[] HIGH_RISK = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 6.5};

    
    //constructor
    public Game(String gameName, String providerName, int stars, int noOfVotes,
                String gameLogo, double minBet, double maxBet, String riskLevel, String hashKey) {

        this.gameName = gameName;
        this.providerName = providerName;
        this.stars = stars;
        this.noOfVotes = noOfVotes;
        this.gameLogo = gameLogo;
        this.minBet = minBet;
        this.maxBet = maxBet;
        this.riskLevel = riskLevel != null ? riskLevel.toLowerCase() : "low";
        this.hashKey = hashKey;

        this.totalBets = 0.0;
        this.totalPayouts = 0.0;

        this.totalBets = 0.0;
        this.totalPayouts = 0.0;
        this.casinoProfitByPlayer = new HashMap<>();    // arxikopoihsh Map
        
        calculateBetCategory();
        calculateJackpot();
    }

    
    private void calculateBetCategory() {
        if (Math.abs(minBet - 0.1) < 0.001) {
            this.betCategory = "$";
        } else if (Math.abs(minBet - 1.0) < 0.001) {
            this.betCategory = "$$";
        } else if (Math.abs(minBet - 5.0) < 0.001) {
            this.betCategory = "$$$";
        } else {
            this.betCategory = "Unknown";
        }
    }

    
    private void calculateJackpot() {
        switch (this.riskLevel) {
            case "low": this.jackpot = 10; break;
            case "medium": this.jackpot = 20; break;
            case "high": this.jackpot = 40; break;
            default: this.jackpot = 10;
        }
    }

    
    public double[] getRiskArray() {
        switch (this.riskLevel) {
            case "low": return LOW_RISK;
            case "medium": return MEDIUM_RISK;
            case "high": return HIGH_RISK;
            default: return LOW_RISK;
        }
    }

    
    // Synchronized katagrafi neou pontarismatos (polla threads pontaroun tautoxrona)
    public synchronized void addBet(String playerName, double amountBet, double amountWon) {
        // enhmerwsh synolikwn statistikwn
        this.totalBets += amountBet;
        this.totalPayouts += amountWon;

        // ypologismos katharou kerdous gia to kazino gia auto to bet
        double netProfitFromThisBet = amountBet - amountWon;

        // enhmerwsh tou istorikou tou paixth -- an o paixths den yparxei sto map bazei 0.0 arxikh timh kai prosthetei neo kerdos
        double currentProfit = casinoProfitByPlayer.getOrDefault(playerName, 0.0);
        casinoProfitByPlayer.put(playerName, currentProfit + netProfitFromThisBet);
    }

    
    // Ypologismos kerdous / zhmias casino
    public synchronized double getCasinoProfit() {
        return this.totalBets - this.totalPayouts;
    }

    
    //getters, setters
    public String getGameName() { return gameName; }
    public String getProviderName() { return providerName; }
    public int getStars() { return stars; }
    public int getNoOfVotes() { return noOfVotes; }
    public String getGameLogo() { return gameLogo; }
    public double getMinBet() { return minBet; }
    public double getMaxBet() { return maxBet; }
    public String getHashKey() { return hashKey; }
    public String getBetCategory() { return betCategory; }
    public int getJackpot() { return jackpot; }
    public String getRiskLevel() { return riskLevel; }

    //epistrefei antigrafo tou map me ta kerdh ana paixth
    public synchronized Map<String, Double> getCasinoProfitByPlayer() {
        return new HashMap<>(casinoProfitByPlayer); 
    }

    public synchronized double getTotalBets() { return totalBets; }
    
    public synchronized double getTotalPayouts() { return totalPayouts; }

    
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel != null ? riskLevel.toLowerCase() : "low";
        calculateJackpot();    // an allaxei to risko allazei kai to jackpot
    }
}
