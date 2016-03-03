package uk.dsxt.voting.common.nxt.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class AccountResponse {

    private final String address;
    private final String publicKey;
    private final int requestProcessingTime;
    private final String accountId;

    @JsonCreator
    public AccountResponse(@JsonProperty("accountRS") String address, @JsonProperty("publicKey") String publicKey,
                           @JsonProperty("requestProcessingTime") int requestProcessingTime, @JsonProperty("account") String accountId) {

        this.address = address;
        this.publicKey = publicKey;
        this.requestProcessingTime = requestProcessingTime;
        this.accountId = accountId;
    }

}
