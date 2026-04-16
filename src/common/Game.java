package common;

import java.io.Serializable;

public class Game implements Serializable {

    private static final long serialVersionUID = 1L;

    // --- 1. Πεδία που διαβάζονται από το JSON ---
    private String gameName;
    private String providerName;
    private int stars;
    private int noOfVotes;
    private String gameLogo;
    private double minBet;
    private double maxBet;
    private String riskLevel;
    private String hashKey; // Αυτό είναι το Secret S για την επικοινωνία με τον SRG

    // --- 2. Πεδία που υπολογίζονται από το σύστημα ---
    private String betCategory;
    private int jackpot;

    // ΠΡΟΣΘΗΚΗ: Συνολικά κέρδη/ζημιές του συστήματος από αυτό το παιχνίδι
    private double totalProfit;

    // --- 3. Σταθερές: Πίνακες Ρίσκου ---
    private static final double[] LOW_RISK = {0.0, 0.0, 0.0, 0.1, 0.5, 1.0, 1.1, 1.3, 2.0, 2.5};
    private static final double[] MEDIUM_RISK = {0.0, 0.0, 0.0, 0.0, 0.0, 0.5, 1.0, 1.5, 2.5, 3.5};
    private static final double[] HIGH_RISK = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 6.5};

    /**
     * Constructor του παιχνιδιού.
     */
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
        this.totalProfit = 0.0; // Αρχικά τα κέρδη είναι 0

        calculateBetCategory();
        calculateJackpot();
    }

    /**
     * Υπολογισμός Κατηγορίας Πονταρίσματος.
     * Tip: Χρησιμοποιούμε Math.abs για τη σύγκριση double ώστε να αποφύγουμε
     * προβλήματα ακρίβειας της Java (π.χ. το 0.1 να αποθηκευτεί ως 0.10000000000001).
     */
    private void calculateBetCategory() {
        if (Math.abs(minBet - 0.1) < 0.001) {
            this.betCategory = "$";
        } else if (Math.abs(minBet - 1.0) < 0.001) {
            this.betCategory = "$$";
        } else if (Math.abs(minBet - 5.0) < 0.001) {
            this.betCategory = "$$$";
        } else {
            this.betCategory = "Unknown"; // Fallback περίπτωση
        }
    }

    /**
     * Υπολογισμός του Jackpot βάσει του riskLevel.
     */
    private void calculateJackpot() {
        switch (this.riskLevel) {
            case "low":
                this.jackpot = 10;
                break;
            case "medium":
                this.jackpot = 20;
                break;
            case "high":
                this.jackpot = 40;
                break;
            default:
                this.jackpot = 10; // Default fallback
        }
    }

    /**
     * Επιστροφή του σωστού πίνακα ρίσκου.
     */
    public double[] getRiskArray() {
        switch (this.riskLevel) {
            case "low": return LOW_RISK;
            case "medium": return MEDIUM_RISK;
            case "high": return HIGH_RISK;
            default: return LOW_RISK;
        }
    }

    // --- ΚΡΙΣΙΜΗ ΠΡΟΣΘΗΚΗ ΓΙΑ ΤΟΝ WORKER ---

    /**
     * Ενημερώνει τα συνολικά κέρδη του παιχνιδιού.
     * Είναι synchronized επειδή πολλαπλά threads (παίκτες)
     * μπορεί να ποντάρουν ταυτόχρονα στο ίδιο παιχνίδι!
     */
    public synchronized void updateProfit(double amount) {
        this.totalProfit += amount;
    }

    // --- 4. Getters και Setters ---

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
    public synchronized double getTotalProfit() { return totalProfit; }

    public String getRiskLevel() { return riskLevel; }

    /**
     * Όταν ο Manager αλλάζει το επίπεδο ρίσκου, πρέπει να
     * επαναϋπολογιστεί αυτόματα και το Jackpot.
     */
    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel != null ? riskLevel.toLowerCase() : "low";
        calculateJackpot();
    }
}