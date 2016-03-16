package uk.dsxt.voting.tests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NodeInfo {
    String nxtPassword;
    int id;
    int ownerId;
    String privateKey;
    
    String holderAPI;
}
