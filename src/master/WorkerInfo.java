package master;

// vohthitikh klash gia apothhkeysh stoixeiwn epikoinwnias (ip, port) tou kathe worker
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