package uk.dsxt.voting.common.nxt.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class PrunableMessages {
    WalletMessage[] prunableMessages;
    int requestProcessingTime;

    @JsonCreator
    public PrunableMessages(@JsonProperty("prunableMessages") WalletMessage[] prunableMessages, @JsonProperty("requestProcessingTime") int requestProcessingTime) {
        this.prunableMessages = prunableMessages;
        this.requestProcessingTime = requestProcessingTime;
    }
}
