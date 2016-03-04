package uk.dsxt.voting.common.networking;

import uk.dsxt.voting.common.messaging.WalletManager;

import java.util.ArrayList;
import java.util.List;

public class MockWalletManager implements WalletManager {

    private static final List<Message> allMessages = new ArrayList<>();

    private static long lastMessageId;

    private boolean isRunning = false;

    @Override
    public void runWallet() {
        isRunning = true;
    }

    @Override
    public void stopWallet() {
        isRunning = false;
    }

    @Override
    public String sendMessage(byte[] body) {
        synchronized (allMessages) {
            String id = String.format("MSG_%d", ++lastMessageId);
            allMessages.add(new Message(id, body));
            return id;
        }
    }

    @Override
    public List<Message> getNewMessages(long timestamp) {
        if (!isRunning)
            return null;
        synchronized (allMessages) {
            return new ArrayList<>(allMessages);
        }
    }
}
