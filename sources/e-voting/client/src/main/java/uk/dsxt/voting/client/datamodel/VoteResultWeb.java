package uk.dsxt.voting.client.datamodel;

import lombok.Value;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.VoteResultStatus;

import java.math.BigDecimal;

@Value
public class VoteResultWeb {
    String votingId;
    String votingName;
    String clientId;
    String clientName;
    BigDecimal packetSize;
    VoteResultStatus status;
    // TODO Add answers for each question

    public VoteResultWeb(VoteResult vr) {
        this.votingId = vr.getVotingId();
        this.votingName = ""; // TODO Get votingName from other sources.
        this.clientId = vr.getHolderId();
        this.clientName = ""; // TODO Get clientName from other sources.
        this.packetSize = vr.getPacketSize();
        this.status = vr.getStatus();
    }
}
