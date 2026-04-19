package request;

import java.io.Serializable;

public class FilterRequest implements Serializable {

    // filter categories (null means we don't use it for the filtering)
    private final String category;    // $, $$, $$$
    private final String provider;    // π.χ. CasinoTech
    private final String riskLevel;   // low, medium, high

    // where the worker sends the results / information for reducer
    private final String reducerHost;
    private final int reducerPort;


    public FilterRequest(String category, String provider, String riskLevel, String reducerHost, int reducerPort) {
        this.category = category;
        this.provider = provider;
        this.riskLevel = riskLevel;
        this.reducerHost = reducerHost;
        this.reducerPort = reducerPort;
    }

    public String getCategory() {
        return category;
    }

    public String getProvider() {
        return provider;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getReducerHost() {
        return reducerHost;
    }

    public int getReducerPort() {
        return reducerPort;
    }
}