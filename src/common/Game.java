package common;

import java.io.Serializable;

/**
 * Η κλάση Game αναπαριστά ένα παιχνίδι στο σύστημα.
 * Υλοποιεί το Serializable ώστε τα αντικείμενα αυτής της κλάσης
 * να μπορούν να ταξιδέψουν μέσω των TCP Sockets (ObjectInputStream/ObjectOutputStream).
 */
public class Game implements Serializable {

    // Καλό είναι να υπάρχει πάντα ένα serialVersionUID όταν στέλνουμε αντικείμενα μέσω δικτύου
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
    private String hashKey;

    // --- 2. Πεδία που υπολογίζονται από το σύστημα ---
    // ΠΡΟΣΟΧΗ: Αυτά τα πεδία ΔΕΝ διαβάζονται από το JSON.
    private String betCategory;
    private int jackpot;

    // --- 3. Σταθερές: Πίνακες Ρίσκου ---
    // TODO: Δηλώστε εδώ τους 3 πίνακες (Low, Medium, High) με τους πολλαπλασιαστές
    // σύμφωνα με την εκφώνηση.


    /**
     * Constructor του παιχνιδιού.
     * Εδώ περνάμε τα δεδομένα που κάναμε parse από το JSON.
     */
    public Game(String gameName, String providerName, int stars, int noOfVotes,
                String gameLogo, double minBet, double maxBet, String riskLevel, String hashKey) {

        // TODO: Αρχικοποιήστε τα πεδία της κλάσης (this.gameName = gameName κλπ.)


        // Αφού πάρουμε τα βασικά δεδομένα, καλούμε τις μεθόδους για
        // να υπολογίσουν τα πεδία που λείπουν.
        calculateBetCategory();
        calculateJackpot();
    }

    /**
     * TODO: Υπολογισμός Κατηγορίας Πονταρίσματος.
     * Πρέπει να ελέγχει το πεδίο minBet και να θέτει το πεδίο betCategory:
     * - Αν minBet είναι 0.1 -> "$"
     * - Αν minBet είναι 1 -> "$$"
     * - Αν minBet είναι 5 -> "$$$"
     */
    private void calculateBetCategory() {
        // Γράψτε τη λογική σας εδώ
    }

    /**
     * TODO: Υπολογισμός του Jackpot.
     * Πρέπει να ελέγχει το πεδίο riskLevel και να θέτει το πεδίο jackpot:
     * - Low -> 10
     * - Medium -> 20
     * - High -> 40
     */
    private void calculateJackpot() {
        // Γράψτε τη λογική σας εδώ
    }

    /**
     * TODO: Επιστροφή του σωστού πίνακα ρίσκου.
     * Αυτή η μέθοδος θα φανεί πολύ χρήσιμη στο Άτομο 3 (Casino Engine)
     * για να υπολογίσει το κέρδος κατά το ποντάρισμα.
     */
    public double[] getRiskArray() {
        // Γράψτε τη λογική σας εδώ: επιστρέψτε τον σωστό πίνακα (Low, Medium ή High)
        // ανάλογα με το riskLevel.
        return null; // Προσωρινό return για να μην χτυπάει σφάλμα
    }

    // --- 4. Getters και Setters ---
    // TODO: Δημιουργήστε τους Getters για να μπορείτε να διαβάζετε τις τιμές (π.χ. getGameName(), getMinBet() κλπ.)


    // TODO: Δημιουργήστε Setter για το riskLevel (και θυμηθείτε να καλείτε
    // ξανά την calculateJackpot() μέσα στον setter, γιατί αν αλλάξει το ρίσκο, αλλάζει και το jackpot!)

}