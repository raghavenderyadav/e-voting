package uk.dsxt.voting.common.datamodel.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class EncryptedMessage {
    String data;
    String nonce;

    @JsonCreator
    public EncryptedMessage(@JsonProperty("data") String data, @JsonProperty("nonce") String nonce) {
        this.data = data;
        this.nonce = nonce;
    }
}
