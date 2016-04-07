package uk.dsxt.voting.common.nxt.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.math.BigDecimal;

@Value
@EqualsAndHashCode(callSuper=false)
public class BlockchainStatusResponse extends BaseWalletResponse {

    private final String application;
    private final String version;
    private final String time;
    private final String lastBlock;
    private final String cumulativeDifficulty;
    private final String numberOfBlocks;
    private final String lastBlockchainFeeder;
    private final String lastBlockchainFeederHeight;
    private final String isScanning;
    private final String isDownloading;
    private final String maxRollback;
    private final String currentMinRollbackHeight;
    private final String isTestnet;
    private final String maxPrunableLifetime;
    private final String includeExpiredPrunable;
    private final String correctInvalidFees;
    private final String[] services;

    @JsonCreator
    public BlockchainStatusResponse(@JsonProperty("application") String application,
                                    @JsonProperty("version") String version,
                                    @JsonProperty("time") String time,
                                    @JsonProperty("lastBlock") String lastBlock,
                                    @JsonProperty("cumulativeDifficulty") String cumulativeDifficulty,
                                    @JsonProperty("numberOfBlocks") String numberOfBlocks,
                                    @JsonProperty("lastBlockchainFeeder") String lastBlockchainFeeder,
                                    @JsonProperty("lastBlockchainFeederHeight") String lastBlockchainFeederHeight,
                                    @JsonProperty("isScanning") String isScanning,
                                    @JsonProperty("isDownloading") String isDownloading,
                                    @JsonProperty("maxRollback") String maxRollback,
                                    @JsonProperty("currentMinRollbackHeight") String currentMinRollbackHeight,
                                    @JsonProperty("isTestnet") String isTestnet,
                                    @JsonProperty("maxPrunableLifetime") String maxPrunableLifetime,
                                    @JsonProperty("includeExpiredPrunable") String includeExpiredPrunable,
                                    @JsonProperty("correctInvalidFees") String correctInvalidFees,
                                    @JsonProperty("services") String[] services,
                                    
                                    @JsonProperty("errorDescription") String errorDescription,
                                    @JsonProperty("errorCode") int errorCode,
                                    @JsonProperty("requestProcessingTime") int requestProcessingTime) {
        super(errorDescription, errorCode, requestProcessingTime);
        this.application = application;
        this.version = version;
        this.time = time;
        this.lastBlock = lastBlock;
        this.cumulativeDifficulty = cumulativeDifficulty;
        this.numberOfBlocks = numberOfBlocks;
        this.lastBlockchainFeeder = lastBlockchainFeeder;
        this.lastBlockchainFeederHeight = lastBlockchainFeederHeight;
        this.isScanning = isScanning;
        this.isDownloading = isDownloading;
        this.maxRollback = maxRollback;
        this.currentMinRollbackHeight = currentMinRollbackHeight;
        this.isTestnet = isTestnet;
        this.maxPrunableLifetime = maxPrunableLifetime;
        this.includeExpiredPrunable = includeExpiredPrunable;
        this.correctInvalidFees = correctInvalidFees;
        this.services = services;
    }
}
