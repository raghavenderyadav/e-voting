package uk.dsxt.voting.tests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    String blacklist;

    @JsonCreator
    public NodeInfo(@JsonProperty("nxtPassword") String nxtPassword, @JsonProperty("id") int id, 
                    @JsonProperty("ownerId") int ownerId, @JsonProperty("privateKey") String privateKey, @JsonProperty("blacklist") String blacklist) {
        this.nxtPassword = nxtPassword;
        this.id = id;
        this.ownerId = ownerId;
        this.privateKey = privateKey;
        this.blacklist = blacklist;
    }
}
