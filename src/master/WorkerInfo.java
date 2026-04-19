package master;

/**
 * vohthitikh klash pou apothhkeuvei ta stoixeia epikoinwnias IP, Port gia kathe Worker node
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