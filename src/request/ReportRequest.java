package request;

import java.io.Serializable;

public class ReportRequest implements Serializable {

    private final String requestId;     // monadiko ID aithmatos
    private final String reportType;    // by_provider h' by_player
    private final String reducerHost;
    private final int reducerPort;

    public ReportRequest(String requestId, String reportType, String reducerHost, int reducerPort) {
        this.requestId = requestId;
        this.reportType = reportType;
        this.reducerHost = reducerHost;
        this.reducerPort = reducerPort;
    }

    public String getRequestId() { return requestId; }
    public String getReportType() { return reportType; }
    public String getReducerHost() { return reducerHost; }
    public int getReducerPort() { return reducerPort; }
}