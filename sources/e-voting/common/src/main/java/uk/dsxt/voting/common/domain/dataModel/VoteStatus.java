package uk.dsxt.voting.common.domain.dataModel;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class VoteStatus {
    String votingId;
    String messageId;
    VoteResultStatus status;
    String voteSign;
    
    @Override
    public String toString() {
        return String.format("%s-%s-%s-%s", votingId, messageId, status, voteSign);
    }
    
    public VoteStatus(String s) {
        if (s == null)
            throw new IllegalArgumentException("VoteStatus can not be created from null string");
        String[] terms = s.split("-");
        if (terms.length == 4) {
            votingId = terms[0];
            messageId = terms[1];
            status = VoteResultStatus.valueOf(terms[2]);
            voteSign = terms[3];
        } else
            throw new IllegalArgumentException(String.format("VoteStatus can not be created from string with %d terms (%s)", terms.length, s));
    }
}
