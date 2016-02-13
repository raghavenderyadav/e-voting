package uk.dsxt.voting.common.datamodel.walletapi;

import nxt.Constants;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class BaseWalletResponse {
    protected BigDecimal longToBigDecimal(long value) {
        return new BigDecimal(value, MathContext.DECIMAL64).divide(new BigDecimal(Constants.ONE_NXT), RoundingMode.DOWN);
    }
}
