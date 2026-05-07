package request;

import java.io.Serializable;

public class FilterRequest implements Serializable {

    private final String requestId;
    private final String category;    // $, $$, $$$
    private final String provider;
    private final String riskLevel;   // low, medium, high
    private final int minStars;       // ΝΕΟ: Ελάχιστα αστέρια
    private final String reducerHost;
    private final int reducerPort;

    public FilterRequest(String requestId, String category, String provider, String riskLevel, int minStars, String reducerHost, int reducerPort) {
        this.requestId = requestId;
        this.category = category;
        this.provider = provider;
        this.riskLevel = riskLevel;
        this.minStars = minStars;
        this.reducerHost = reducerHost;
        this.reducerPort = reducerPort;
    }

    public String getRequestId() { return requestId; }
    public String getCategory() { return category; }
    public String getProvider() { return provider; }
    public String getRiskLevel() { return riskLevel; }
    public int getMinStars() { return minStars; }
    public String getReducerHost() { return reducerHost; }
    public int getReducerPort() { return reducerPort; }
}