package uk.dsxt.voting.common.datamodel.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class TransactionsResponse extends BaseWalletResponse {

    Transaction[] transactions;

    @JsonCreator
    protected TransactionsResponse(@JsonProperty("transactions") Transaction[] transactions,
                                   @JsonProperty("errorDescription") String errorDescription,
                                   @JsonProperty("errorCode") int errorCode,
                                   @JsonProperty("requestProcessingTime") int requestProcessingTime) {
        super(errorDescription, errorCode, requestProcessingTime);
        this.transactions = transactions;
    }
}
