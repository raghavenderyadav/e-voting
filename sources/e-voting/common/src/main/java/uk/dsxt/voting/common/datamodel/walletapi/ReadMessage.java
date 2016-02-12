package uk.dsxt.voting.common.datamodel.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class ReadMessage {
    int requestProcessingTime;
    String message;

    @JsonCreator
    public ReadMessage(@JsonProperty("requestProcessingTime") int requestProcessingTime, @JsonProperty("message") String message) {
        this.requestProcessingTime = requestProcessingTime;
        this.message = message;
    }
}
