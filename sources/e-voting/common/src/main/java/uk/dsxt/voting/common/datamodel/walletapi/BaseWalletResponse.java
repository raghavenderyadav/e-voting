package uk.dsxt.voting.common.datamodel.walletapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import nxt.Constants;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;


public class BaseWalletResponse {
    @Getter
    private String errorDescription;
    @Getter
    private int errorCode;
    @Getter
    private int requestProcessingTime;

    @JsonCreator
    protected BaseWalletResponse(@JsonProperty("errorDescription") String errorDescription, @JsonProperty("errorCode") int errorCode,
                                 @JsonProperty("requestProcessingTime") int requestProcessingTime) {
        this.errorDescription = errorDescription;
        this.errorCode = errorCode;
        this.requestProcessingTime = requestProcessingTime;
    }

    protected BigDecimal longToBigDecimal(long value) {
        return new BigDecimal(value, MathContext.DECIMAL64).divide(new BigDecimal(Constants.ONE_NXT), RoundingMode.DOWN);
    }
}
