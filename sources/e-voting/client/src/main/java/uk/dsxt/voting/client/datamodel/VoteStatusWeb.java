package uk.dsxt.voting.client.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import uk.dsxt.voting.common.domain.dataModel.VoteResultStatus;
import uk.dsxt.voting.common.domain.dataModel.VoteStatus;

@Value
public class VoteStatusWeb {
    String messageId;
    VoteResultStatus status;
    @JsonProperty("signature")
    String voteSign;

    @JsonCreator
    public VoteStatusWeb(@JsonProperty("messageId") String messageId, @JsonProperty("status") VoteResultStatus status, @JsonProperty("signature") String voteSign) {
        this.messageId = messageId;
        this.status = status;
        this.voteSign = voteSign;
    }
    
    public VoteStatusWeb(VoteStatus v) {
        this.messageId = v.getMessageId();
        this.status = v.getStatus();
        this.voteSign = v.getVoteSign();
    }
}
