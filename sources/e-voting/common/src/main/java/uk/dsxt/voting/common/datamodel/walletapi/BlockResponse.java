package uk.dsxt.voting.common.datamodel.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.math.BigDecimal;

@Value
public class BlockResponse extends BaseWalletResponse {
    String previousBlockHash;
    int payloadLength;
    BigDecimal totalAmount;
    String generationSignature;
    String generator;
    String generatorPublicKey;
    String baseTarget;
    String payloadHash;
    String generatorRS;
    String nextBlock;
    int requestProcessingTime;
    int numberOfTransactions;
    String blockSignature;
    String[] transactions;
    int version;
    BigDecimal totalFee;
    String previousBlock;
    String block;
    int height;
    long timestamp;

    @JsonCreator
    public BlockResponse(@JsonProperty("previousBlockHash") String previousBlockHash, @JsonProperty("payloadLength") int payloadLength,
                         @JsonProperty("totalAmountNQT") long totalAmountNQT, @JsonProperty("generationSignature") String generationSignature,
                         @JsonProperty("generator") String generator, @JsonProperty("generatorPublicKey") String generatorPublicKey,
                         @JsonProperty("baseTarget") String baseTarget, @JsonProperty("payloadHash") String payloadHash,
                         @JsonProperty("generatorRS") String generatorRS, @JsonProperty("nextBlock") String nextBlock,
                         @JsonProperty("requestProcessingTime") int requestProcessingTime, @JsonProperty("numberOfTransactions") int numberOfTransactions,
                         @JsonProperty("blockSignature") String blockSignature, @JsonProperty("transactions") String[] transactions,
                         @JsonProperty("version") int version, @JsonProperty("totalFeeNQT") long totalFeeNQT,
                         @JsonProperty("previousBlock") String previousBlock, @JsonProperty("block") String block,
                         @JsonProperty("height") int height, @JsonProperty("timestamp") long timestamp) {
        this.previousBlockHash = previousBlockHash;
        this.payloadLength = payloadLength;
        this.totalAmount = longToBigDecimal(totalAmountNQT);
        this.generationSignature = generationSignature;
        this.generator = generator;
        this.generatorPublicKey = generatorPublicKey;
        this.baseTarget = baseTarget;
        this.payloadHash = payloadHash;
        this.generatorRS = generatorRS;
        this.nextBlock = nextBlock;
        this.requestProcessingTime = requestProcessingTime;
        this.numberOfTransactions = numberOfTransactions;
        this.blockSignature = blockSignature;
        this.transactions = transactions;
        this.version = version;
        this.totalFee = longToBigDecimal(totalFeeNQT);
        this.previousBlock = previousBlock;
        this.block = block;
        this.height = height;
        this.timestamp = timestamp;
    }
}
