package request;

import java.io.Serializable;

public class ReportRequest implements Serializable {

    private final String reportType;    // by_provider or by_player

    // where the worker sends the results / information for reducer
    private final String reducerHost;
    private final int reducerPort;


    public ReportRequest(String reportType, String reducerHost, int reducerPort) {
        this.reportType = reportType;
        this.reducerHost = reducerHost;
        this.reducerPort = reducerPort;
    }

    public String getReportType() { return reportType; }

    public String getReducerHost() {
        return reducerHost;
    }

    public int getReducerPort() {
        return reducerPort;
    }
}
