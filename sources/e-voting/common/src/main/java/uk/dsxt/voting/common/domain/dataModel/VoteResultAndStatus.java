package uk.dsxt.voting.common.domain.dataModel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
public class VoteResultAndStatus {
    @Getter
    @Setter
    private VoteResult result;

    @Getter
    @Setter
    private VoteStatus status;
    
    @Getter
    @Setter
    private ClientVoteReceipt receipt;
}
