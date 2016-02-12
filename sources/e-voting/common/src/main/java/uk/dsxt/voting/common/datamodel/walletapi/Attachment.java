package uk.dsxt.voting.common.datamodel.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    boolean messageIsText;
    String message;

    @JsonCreator
    public Attachment(@JsonProperty("phasingFinishHeight") int phasingFinishHeight, @JsonProperty("phasingHolding") int phasingHolding,
                      @JsonProperty("phasingQuorum") int phasingQuorum, @JsonProperty("version.Phasing") int version,
                      @JsonProperty("phasingMinBalance") int phasingMinBalance, @JsonProperty("phasingMinBalanceModel") int phasingMinBalanceModel,
                      @JsonProperty("version.OrdinaryPayment") int versionOrdinaryPayment, @JsonProperty("phasingVotingModel") int phasingVotingModel,
                      @JsonProperty("version.Message") int versionMessage, @JsonProperty("messageIsText") boolean messageIsText, @JsonProperty("message") String message) {
        this.phasingFinishHeight = phasingFinishHeight;
        this.phasingHolding = phasingHolding;
        this.phasingQuorum = phasingQuorum;
        this.versionPhasing = version;
        this.phasingMinBalance = phasingMinBalance;
        this.phasingMinBalanceModel = phasingMinBalanceModel;
        this.versionOrdinaryPayment = versionOrdinaryPayment;
        this.phasingVotingModel = phasingVotingModel;
        this.versionMessage = versionMessage;
        this.messageIsText = messageIsText;
        this.message = message;
    }
}
