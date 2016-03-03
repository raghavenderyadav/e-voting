package uk.dsxt.voting.common.nxt.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper=false)
public class StartForgingResponse extends BaseWalletResponse {
    long deadline;
    long hitTime;

    @JsonCreator
    public StartForgingResponse(@JsonProperty("requestProcessingTime") int requestProcessingTime,
                                @JsonProperty("deadline") long deadline, @JsonProperty("hitTime") long hitTime,
                                @JsonProperty("errorDescription") String errorDescription, @JsonProperty("errorCode") int errorCode) {
        super(errorDescription, errorCode, requestProcessingTime);
        this.deadline = deadline;
        this.hitTime = hitTime;
    }
}
