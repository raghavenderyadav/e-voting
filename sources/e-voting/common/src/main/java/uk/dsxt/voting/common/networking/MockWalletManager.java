package uk.dsxt.voting.common.networking;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.messaging.Message;

import java.util.ArrayList;
import java.util.List;

@Log4j2
public class MockWalletManager implements WalletManager {

    private static final List<Message> allMessages = new ArrayList<>();

    private static long lastMessageId;

    private boolean isRunning = false;

    @Override
    public void start() {
        log.info("connector started");
        isRunning = true;
    }

    @Override
    public void stop() {
        log.info("connector stopped");
        isRunning = false;
    }

    @Override
    public String sendMessage(byte[] body) {
        if (!isRunning)
            return null;
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
