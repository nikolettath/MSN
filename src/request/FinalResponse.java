package request;

import java.io.Serializable;

public class FinalResponse implements Serializable {
    private final String requestId;
    private final Object results;       // List<Game> h' Map<String, Double>

    public FinalResponse(String requestId, Object results) {
        this.requestId = requestId;
        this.results = results;
    }

    public String getRequestId() { return requestId; }
    public Object getResults() { return results; }
}
