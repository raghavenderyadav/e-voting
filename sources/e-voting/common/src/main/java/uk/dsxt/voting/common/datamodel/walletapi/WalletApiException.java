package uk.dsxt.voting.common.datamodel.walletapi;

public class WalletApiException extends RuntimeException {

    public WalletApiException(String message) {
        super(message);
    }

    public WalletApiException(String message, Throwable t) {
        super(message, t);
    }

}
