package uk.dsxt.voting.common.datamodel.walletapi;

public enum WalletRequestType {

    GET_BALANCE ("getBalance"),
    GET_ACCOUNT_ID ("getAccountId"),
    GET_UNCONFIRMED_TRANSACTIONS("getUnconfirmedTransactions"),
    GET_BLOCKCHAIN_TRANSACTIONS ("getBlockchainTransactions"),
    SEND_MESSAGE("sendMessage"),
    READ_MESSAGE("readMessage"),
    SEND_MONEY("sendTransaction"),
    GET_TRANSACTION("getTransaction"),
    START_FORGING("startForging"),
    GET_BLOCK("getBlock");

    private final String strValue;

    WalletRequestType(String strValue) {
        this.strValue = strValue;
    }

    @Override
    public String toString() {
        return strValue;
    }
}
