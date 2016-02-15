package uk.dsxt.voting.common.datamodel.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class StartForgingResponse {
    int requestProcessingTime;
    int deadline;
    int hitTime;

    @JsonCreator
    public StartForgingResponse(@JsonProperty("requestProcessingTime") int requestProcessingTime,
                                @JsonProperty("deadline") int deadline, @JsonProperty("hitTime") int hitTime) {
        this.requestProcessingTime = requestProcessingTime;
        this.deadline = deadline;
        this.hitTime = hitTime;
    }
}
