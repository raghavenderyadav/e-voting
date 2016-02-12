package uk.dsxt.voting.common.datamodel.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class SendTransactionResponse {

    String signatureHash;
    String unsignedTransactionBytes;
    Transaction transactionJSON;
    boolean broadCasted;
    long requestProcessingTime;
    String transactionBytes;
    String fullHash;
    String transactionId;

    @JsonCreator
    public SendTransactionResponse(@JsonProperty("signatureHash") String signatureHash, @JsonProperty("unsignedTransactionBytes") String unsignedTransactionBytes,
                                   @JsonProperty("transactionJSON") Transaction transactionJSON, @JsonProperty("broadcasted") boolean broadcasted,
                                   @JsonProperty("requestProcessingTime") long requestProcessingTime, @JsonProperty("transactionBytes") String transactionBytes,
                                   @JsonProperty("fullHash") String fullHash, @JsonProperty("transaction") String transaction) {
        this.signatureHash = signatureHash;
        this.unsignedTransactionBytes = unsignedTransactionBytes;
        this.transactionJSON = transactionJSON;
        this.broadCasted = broadcasted;
        this.requestProcessingTime = requestProcessingTime;
        this.transactionBytes = transactionBytes;
        this.fullHash = fullHash;
        this.transactionId = transaction;
    }
}
