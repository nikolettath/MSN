package request;

import java.io.Serializable;

// klash gia thn telikh apanthsh pou stelnei o reducer ston master mesw tou monitor
public class FinalResponse implements Serializable {
    private final String requestId;
    // mporei na einai List<Game> gia anazhthsh h Map<String, Double> gia report
    private final Object results;

    public FinalResponse(String requestId, Object results) {
        this.requestId = requestId;
        this.results = results;
    }

    public String getRequestId() { return requestId; }
    public Object getResults() { return results; }
}