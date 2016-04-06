package uk.dsxt.voting.common.domain.dataModel;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class VoteStatus {
    String votingId;
    String messageId;
    VoteResultStatus status;
    String voteDigest;
    String voteSign;
    
    @Override
    public String toString() {
        return String.format("%s_%s_%s_%s_%s", votingId, messageId, status, voteDigest, voteSign);
    }
    
    public VoteStatus(String s) {
        if (s == null)
            throw new IllegalArgumentException("VoteStatus can not be created from null string");
        String[] terms = s.split("_");
        if (terms.length == 5) {
            votingId = terms[0];
            messageId = terms[1];
            status = VoteResultStatus.valueOf(terms[2]);
            voteDigest = terms[3];
            voteSign = terms[4];
        } else
            throw new IllegalArgumentException(String.format("VoteStatus can not be created from string with %d terms (%s)", terms.length, s));
    }
}
