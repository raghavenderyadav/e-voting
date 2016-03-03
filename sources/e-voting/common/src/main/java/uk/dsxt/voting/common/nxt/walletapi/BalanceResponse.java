package uk.dsxt.voting.common.nxt.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.math.BigDecimal;

@Value
@EqualsAndHashCode(callSuper=false)
public class BalanceResponse extends BaseWalletResponse {

    private final BigDecimal unconfirmedBalance;
    private final BigDecimal guaranteedBalance;
    private final BigDecimal effectiveBalance;
    private final BigDecimal forgedBalance;
    private final BigDecimal balance;

    @JsonCreator
    public BalanceResponse(@JsonProperty("unconfirmedBalanceNQT") long unconfirmedBalanceNQT,
                           @JsonProperty("guaranteedBalanceNQT") long guaranteedBalanceNQT,
                           @JsonProperty("effectiveBalanceNXT") long effectiveBalanceNXT,
                           @JsonProperty("forgedBalanceNQT") long forgedBalanceNQT,
                           @JsonProperty("balanceNQT") long balanceNQT,
                           @JsonProperty("errorDescription") String errorDescription,
                           @JsonProperty("errorCode") int errorCode,
                           @JsonProperty("requestProcessingTime") int requestProcessingTime) {
        super(errorDescription, errorCode, requestProcessingTime);
        this.unconfirmedBalance = longToBigDecimal(unconfirmedBalanceNQT);
        this.guaranteedBalance = longToBigDecimal(guaranteedBalanceNQT);
        this.effectiveBalance = longToBigDecimal(effectiveBalanceNXT);
        this.forgedBalance = longToBigDecimal(forgedBalanceNQT);
        this.balance = longToBigDecimal(balanceNQT);
    }
}
