package uk.dsxt.voting.common.datamodel.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.math.BigDecimal;

@Value
@EqualsAndHashCode(callSuper=false)
public class Transaction extends BaseWalletResponse {
    String senderPublicKey;
    String signature;
    BigDecimal fee;
    int type;
    String fullHash;
    int version;
    boolean phased;
    String ecBlockId;
    String signatureHash;
    Attachment attachment;
    String senderRS;
    int subtype;
    BigDecimal amount;
    String sender;
    String recipientRS;
    String recipient;
    int ecBlockHeight;
    int deadline;
    String transaction;
    int timestamp;
    int height;

    @JsonCreator
    public Transaction(@JsonProperty("senderPublicKey") String senderPublicKey, @JsonProperty("signature") String signature,
                       @JsonProperty("feeNQT") long feeNQT, @JsonProperty("type") int type, @JsonProperty("fullHash") String fullHash,
                       @JsonProperty("version") int version, @JsonProperty("phased") boolean phased, @JsonProperty("ecBlockId") String ecBlockId,
                       @JsonProperty("signatureHash") String signatureHash, @JsonProperty("attachment") Attachment attachment,
                       @JsonProperty("senderRS") String senderRS, @JsonProperty("subtype") int subtype, @JsonProperty("amountNQT") long amountNQT,
                       @JsonProperty("sender") String sender, @JsonProperty("recipientRS") String recipientRS,
                       @JsonProperty("recipient") String recipient, @JsonProperty("ecBlockHeight") int ecBlockHeight,
                       @JsonProperty("deadline") int deadline, @JsonProperty("transaction") String transaction,
                       @JsonProperty("timestamp") int timestamp, @JsonProperty("height") int height) {
        this.senderPublicKey = senderPublicKey;
        this.signature = signature;
        this.fee = longToBigDecimal(feeNQT);
        this.type = type;
        this.fullHash = fullHash;
        this.version = version;
        this.phased = phased;
        this.ecBlockId = ecBlockId;
        this.signatureHash = signatureHash;
        this.attachment = attachment;
        this.senderRS = senderRS;
        this.subtype = subtype;
        this.amount = longToBigDecimal(amountNQT);
        this.sender = sender;
        this.recipientRS = recipientRS;
        this.recipient = recipient;
        this.ecBlockHeight = ecBlockHeight;
        this.deadline = deadline;
        this.transaction = transaction;
        this.timestamp = timestamp;
        this.height = height;
    }
}
