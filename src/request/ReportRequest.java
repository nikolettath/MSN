package request;

import java.io.Serializable;

// klash gia thn apostolh aithmatos gia oikonomika statistika
public class ReportRequest implements Serializable {

    private final String requestId;     // monadiko id aithmatos
    private final String reportType;    // typos report (ana paroxo h ana paikth)
    private final String reducerHost;
    private final int reducerPort;

    public ReportRequest(String requestId, String reportType, String reducerHost, int reducerPort) {
        this.requestId = requestId;
        this.reportType = reportType;
        this.reducerHost = reducerHost;
        this.reducerPort = reducerPort;
    }

    // getters gia thn ektelesh tou report apo ton worker
    public String getRequestId() { return requestId; }
    public String getReportType() { return reportType; }
    public String getReducerHost() { return reducerHost; }
    public int getReducerPort() { return reducerPort; }
}