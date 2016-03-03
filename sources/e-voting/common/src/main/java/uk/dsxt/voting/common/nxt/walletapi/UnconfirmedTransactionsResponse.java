package uk.dsxt.voting.common.nxt.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper=false)
public class UnconfirmedTransactionsResponse extends BaseWalletResponse {

    Transaction[] unconfirmedTransactions;

    @JsonCreator
    protected UnconfirmedTransactionsResponse(@JsonProperty("unconfirmedTransactions") Transaction[] unconfirmedTransactions,
                                              @JsonProperty("errorDescription") String errorDescription,
                                              @JsonProperty("errorCode") int errorCode,
                                              @JsonProperty("requestProcessingTime") int requestProcessingTime) {
        super(errorDescription, errorCode, requestProcessingTime);
        this.unconfirmedTransactions = unconfirmedTransactions;
    }
}
