package uk.dsxt.voting.common.networking;

import java.math.BigDecimal;
import java.util.List;

public class MockWalletManager implements WalletManager {
    @Override
    public void runWallet() {

    }

    @Override
    public void stopWallet() {

    }

    @Override
    public String sendMessage(byte[] body) {
        return null;
    }

    @Override
    public List<Message> getNewMessages(long timestamp) {
        return null;
    }
}
