package master;

/**
 * Βοηθητική κλάση που αποθηκεύει τα στοιχεία επικοινωνίας (IP, Port)
 * για τον κάθε Worker Node.
 */
public class WorkerInfo {
    private String ip;
    private int port;

    public WorkerInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}