package common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Game implements Serializable {
    private static final long serialVersionUID = 1L;
    private String gameName;
    private String providerName;
    private int stars;
    private int noOfVotes;
    private String gameLogo;
    private double minBet;
    private double maxBet;
    private String riskLevel;
    private String hashKey;
    private boolean active;

    private String betCategory;
    private int jackpot;
    private double totalBets;
    private double totalPayouts;
    // map gia apothhkeysh kerdous kazino ana paikth
    private final Map<String, Double> casinoProfitByPlayer;
    private String srgIp;

    // pinakes pollaplasiastwn vash riskou
    private static final double[] LOW_RISK = {0.0, 0.0, 0.0, 0.1, 0.5, 1.0, 1.1, 1.3, 2.0, 2.5};
    private static final double[] MEDIUM_RISK = {0.0, 0.0, 0.0, 0.0, 0.0, 0.5, 1.0, 1.5, 2.5, 3.5};
    private static final double[] HIGH_RISK = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 6.5};

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
        this.active = true;
        this.totalBets = 0.0;
        this.totalPayouts = 0.0;
        this.casinoProfitByPlayer = new HashMap<>();
        // aytomatos ypologismos field pou den yparxoun sto json
        calculateBetCategory();
        calculateJackpot();
    }

    // ypologismos kathgorias pontarismatos vash elaxistou oriou
    private void calculateBetCategory() {
        if (minBet < 1.0) this.betCategory = "$";
        else if (minBet < 5.0) this.betCategory = "$$";
        else this.betCategory = "$$$";
    }

    // ypologismos jackpot vash riskou
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
            case "medium": return MEDIUM_RISK;
            case "high": return HIGH_RISK;
            default: return LOW_RISK;
        }
    }

    // synchronized setters gia asfalh allagh apo polla threads
    public synchronized void setRiskLevel(String newRiskLevel) {
        this.riskLevel = newRiskLevel.toLowerCase();
        calculateJackpot();
    }

    public synchronized void setMinBet(double minBet) {
        this.minBet = minBet;
        calculateBetCategory();
    }

    public synchronized void setMaxBet(double maxBet) {
        this.maxBet = maxBet;
    }

    // prosthkh vathmologias kai epanaypologismos mesou orou
    public synchronized void addRating(int newStars) {
        if (newStars >= 1 && newStars <= 5) {
            double currentTotal = this.stars * this.noOfVotes;
            this.noOfVotes++;
            this.stars = (int) Math.round((currentTotal + newStars) / this.noOfVotes);
        }
    }

    // enhmerwsh tzirou kai ypologismos kerdous kazino apo sugkekrimeno paikth
    public synchronized void addBet(String playerName, double amountBet, double amountWon) {
        this.totalBets += amountBet;
        this.totalPayouts += amountWon;
        // katharo kerdos tou kazino apo to pontarisma
        double netProfitFromThisBet = amountBet - amountWon;
        double currentProfit = casinoProfitByPlayer.getOrDefault(playerName, 0.0);
        casinoProfitByPlayer.put(playerName, currentProfit + netProfitFromThisBet);
    }

    public synchronized double getCasinoProfit() { return this.totalBets - this.totalPayouts; }

    // getters
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
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getSrgIp() { return srgIp; }
    public void setSrgIp(String srgIp) { this.srgIp = srgIp; }

    // asfalhs epistrofh antigrafou tou map me ta kerdi ana paikth
    public synchronized Map<String, Double> getCasinoProfitByPlayer() {
        return new HashMap<>(casinoProfitByPlayer);
    }
}