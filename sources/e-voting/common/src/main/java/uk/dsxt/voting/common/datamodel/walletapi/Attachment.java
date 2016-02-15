package uk.dsxt.voting.common.datamodel.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class Attachment {
    int phasingFinishHeight;
    int phasingHolding;
    int phasingQuorum;
    int versionPhasing;
    int phasingMinBalance;
    int phasingMinBalanceModel;
    int versionOrdinaryPayment;
    int phasingVotingModel;
    int versionMessage;
    int versionArbitraryMessage;
    boolean messageIsText;
    String message;

    @JsonCreator
    public Attachment(@JsonProperty("phasingFinishHeight") int phasingFinishHeight, @JsonProperty("phasingHolding") int phasingHolding,
                      @JsonProperty("phasingQuorum") int phasingQuorum, @JsonProperty("version.Phasing") int version,
                      @JsonProperty("phasingMinBalance") int phasingMinBalance, @JsonProperty("phasingMinBalanceModel") int phasingMinBalanceModel,
                      @JsonProperty("version.OrdinaryPayment") int versionOrdinaryPayment, @JsonProperty("phasingVotingModel") int phasingVotingModel,
                      @JsonProperty("version.Message") int versionMessage, @JsonProperty("version.ArbitraryMessage") int versionArbitraryMessage,
                      @JsonProperty("messageIsText") boolean messageIsText, @JsonProperty("message") String message) {
        this.phasingFinishHeight = phasingFinishHeight;
        this.phasingHolding = phasingHolding;
        this.phasingQuorum = phasingQuorum;
        this.versionPhasing = version;
        this.phasingMinBalance = phasingMinBalance;
        this.phasingMinBalanceModel = phasingMinBalanceModel;
        this.versionOrdinaryPayment = versionOrdinaryPayment;
        this.phasingVotingModel = phasingVotingModel;
        this.versionMessage = versionMessage;
        this.versionArbitraryMessage = versionArbitraryMessage;
        this.messageIsText = messageIsText;
        this.message = message;
    }
}
