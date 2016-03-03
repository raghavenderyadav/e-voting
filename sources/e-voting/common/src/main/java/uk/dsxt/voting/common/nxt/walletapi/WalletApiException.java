package uk.dsxt.voting.common.nxt.walletapi;

public class WalletApiException extends RuntimeException {

    public WalletApiException(String message) {
        super(message);
    }

    public WalletApiException(String message, Throwable t) {
        super(message, t);
    }

}
