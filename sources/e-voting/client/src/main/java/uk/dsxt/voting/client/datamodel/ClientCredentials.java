package uk.dsxt.voting.client.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientCredentials {
    String clientId;
    String password;
    UserRole role;

    @JsonCreator
    public ClientCredentials(@JsonProperty("clientId") String clientId, @JsonProperty("password") String password, @JsonProperty("role") UserRole role) {
        this.clientId = clientId;
        this.password = password;
        this.role = role == null ? UserRole.VOTER : role;
    }

    public ClientCredentials(String clientId, String password) {
        this(clientId, password, null);
    }
}
