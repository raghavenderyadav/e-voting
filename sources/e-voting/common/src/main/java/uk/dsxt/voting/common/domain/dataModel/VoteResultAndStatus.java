package uk.dsxt.voting.common.domain.dataModel;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class VoteResultAndStatus {
    VoteResult result;
    VoteStatus status;
}
