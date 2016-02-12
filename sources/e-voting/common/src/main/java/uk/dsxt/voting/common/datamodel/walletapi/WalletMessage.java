package uk.dsxt.voting.common.datamodel.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class WalletMessage {
    String senderRS;
    EncryptedMessage encryptedMessage;
    String sender;
    String recipientRS;
    String recipient;
    int blockTimestamp;
    String transaction;
    boolean isText;
    int transactionTimestamp;
    boolean isCompressed;

    @JsonCreator
    public WalletMessage(@JsonProperty("senderRS") String senderRS, @JsonProperty("encryptedMessage") EncryptedMessage encryptedMessage,
                         @JsonProperty("sender") String sender, @JsonProperty("recipientRS") String recipientRS,
                         @JsonProperty("recipient") String recipient, @JsonProperty("blockTimestamp") int blockTimestamp,
                         @JsonProperty("transaction") String transaction, @JsonProperty("isText") boolean isText,
                         @JsonProperty("transactionTimestamp") int transactionTimestamp, @JsonProperty("isCompressed") boolean isCompressed) {
        this.senderRS = senderRS;
        this.encryptedMessage = encryptedMessage;
        this.sender = sender;
        this.recipientRS = recipientRS;
        this.recipient = recipient;
        this.blockTimestamp = blockTimestamp;
        this.transaction = transaction;
        this.isText = isText;
        this.transactionTimestamp = transactionTimestamp;
        this.isCompressed = isCompressed;
    }
}
