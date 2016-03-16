package uk.dsxt.voting.tests;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.dsxt.voting.common.domain.dataModel.ParticipantRole;

@Data
@AllArgsConstructor
public class NodeInfo {
    String nxtPassword;
    int id;
    int ownerId;
    String privateKey;
    ParticipantRole role;
    
    String holderAPI;
}
