package uk.dsxt.voting.client.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import uk.dsxt.voting.common.domain.dataModel.ClientVoteReceipt;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VoteReceiptWeb {
    @JsonProperty("message")
    String voteResultMessage;

    @JsonProperty("id")
    String transactionId;

    Long timestamp;

    String signature;

    @JsonCreator
    public VoteReceiptWeb(@JsonProperty("message") String voteResultMessage, @JsonProperty("id") String transactionId,
                         @JsonProperty("timestamp") Long timestamp, @JsonProperty("signature") String signature) {
        this.voteResultMessage = voteResultMessage;
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.signature = signature;
    }
    
    public VoteReceiptWeb(ClientVoteReceipt receipt) {
        this.voteResultMessage = receipt.getVoteResultMessage();
        this.transactionId = receipt.getTransactionId();
        this.timestamp = receipt.getTimestamp();
        this.signature = receipt.getSignature();
    }
}
