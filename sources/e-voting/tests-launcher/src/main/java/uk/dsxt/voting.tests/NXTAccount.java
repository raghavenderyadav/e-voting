package uk.dsxt.voting.tests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NXTAccount {
    String account;
    String password;

    @JsonCreator
    public NXTAccount(@JsonProperty("account") String account, @JsonProperty("password") String password) {
        this.account = account;
        this.password = password;
    }
}
