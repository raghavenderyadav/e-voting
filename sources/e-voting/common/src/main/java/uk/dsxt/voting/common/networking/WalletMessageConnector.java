/******************************************************************************
 * e-voting system                                                            *
 * Copyright (C) 2016 DSX Technologies Limited.                               *
 * *
 * This program is free software; you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation; either version 2 of the License, or          *
 * (at your option) any later version.                                        *
 * *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied                         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * *
 * You can find copy of the GNU General Public License in LICENSE.txt file    *
 * at the top-level directory of this distribution.                           *
 * *
 * Removal or modification of this copyright notice is prohibited.            *
 * *
 ******************************************************************************/

package uk.dsxt.voting.common.networking;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.joda.time.Instant;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.VoteStatus;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.domain.nodes.*;
import uk.dsxt.voting.common.messaging.MessageContent;
import uk.dsxt.voting.common.messaging.MessagesSerializer;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.MessageBuilder;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@AllArgsConstructor
@Log4j2
public class WalletMessageConnector implements NetworkMessagesSender {

    private static final String TYPE_VOTE = "VOTE";
    private static final String TYPE_VOTE_STATUS = "VOTE_STATUS";
    private static final String TYPE_VOTING = "VOTING";
    private static final String TYPE_VOTING_TOTAL_RESULT = "VOTING_TOTAL_RESULT";

    private static final String FIELD_BODY = "BODY";

    private final WalletManager walletManager;

    private final MessagesSerializer serializer;

    private final CryptoHelper cryptoHelper;

    private final Map<String, PublicKey> participantKeysById;

    private final PrivateKey privateKey;

    private final String holderId;

    private final String masterId;

    private final List<NetworkMessagesReceiver> messageReceivers = new ArrayList<>();

    private final PublicKey masterKey;

    private final ScheduledExecutorService unconfirmedMessagesChecker = Executors.newSingleThreadScheduledExecutor();

    private static class MessageRecord {
        long timestamp;
        String uid;
        byte[] body;
    }

    private final Map<String, MessageRecord> unconfirmedMessages = new HashMap<>();

    private final long confirmTimeout;

    private final AtomicLong sentMessageCount = new AtomicLong();
    private final AtomicLong sentMessageTryCount = new AtomicLong();
    private final AtomicLong receivedMessageCount = new AtomicLong();
    private final AtomicLong receivedSelfMessageCount = new AtomicLong();

    public WalletMessageConnector(WalletManager walletManager, MessagesSerializer serializer, CryptoHelper cryptoHelper, Map<String, PublicKey> participantKeysById,
                                  PrivateKey privateKey, String holderId, String masterId, long confirmTimeout) {
        this.walletManager = walletManager;
        this.serializer = serializer;
        this.cryptoHelper = cryptoHelper;
        this.participantKeysById = participantKeysById;
        this.privateKey = privateKey;
        this.holderId = holderId;
        this.masterId = masterId;
        this.confirmTimeout = confirmTimeout;
        this.masterKey = participantKeysById.get(masterId);
        unconfirmedMessagesChecker.scheduleWithFixedDelay(this::checkUnconfirmedMessages, 1, 1, TimeUnit.MINUTES);
    }

    public void addClient(NetworkClient client) {
        messageReceivers.add(client);
        client.setNetworkMessagesSender(this);
    }

    @Override
    public String addVoting(Voting voting) {
        return send(TYPE_VOTING, serializer.serialize(voting));
    }

    @Override
    public String addVotingTotalResult(VoteResult result, Voting voting) {
        String body;
        try {
            body = serializer.serialize(result, voting);
        } catch (InternalLogicException e) {
            log.error("addVotingTotalResult. Serialization failed. holderId={} votingId={}", result.getHolderId(), result.getVotingId());
            return null;
        }
        return send(TYPE_VOTING_TOTAL_RESULT, body);
    }

    @Override
    public String addVoteStatus(VoteStatus status) {
        String id = send(TYPE_VOTE_STATUS, serializer.serialize(status));
        log.debug("addVoteStatus. vote messageId={} status={} status messageId={}", status.getMessageId(), status.getStatus(), id);
        return id;
    }

    @Override
    public String addVote(VoteResult result, String serializedVote, String ownerSignature) {
        String message = MessageBuilder.buildMessage(serializedVote, ownerSignature);
        String encryptedMessage;
        try {
            encryptedMessage = cryptoHelper.encrypt(message, masterKey);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            log.error("addVote. can not encrypt message. receiverId={}. error={} holderId={}", masterId, e.getMessage(), holderId);
            return null;
        }
        return send(TYPE_VOTE, encryptedMessage);
    }

    private String send(String messageType, String messageBody) {
        Map<String, String> fields = new HashMap<>();
        fields.put(FIELD_BODY, messageBody);
        MessageRecord messageRecord = new MessageRecord();
        try {
            messageRecord.body = MessageContent.buildOutputMessage(messageType, holderId, privateKey, cryptoHelper, fields);
            sentMessageCount.incrementAndGet();
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            log.error("send {} fails: {}. holderId={}", messageType, e.getMessage(), holderId);
            return null;
        }
        messageRecord.uid = fields.get(MessageContent.FIELD_UID);
        send(messageRecord);
        synchronized (unconfirmedMessages) {
            unconfirmedMessages.put(messageRecord.uid, messageRecord);
        }
        return messageRecord.uid;
    }
    
    private void send(MessageRecord messageRecord) {
        messageRecord.timestamp = System.currentTimeMillis();
        sentMessageTryCount.incrementAndGet();
        String id = walletManager.sendMessage(messageRecord.body);
        if (id == null) {
            log.error("send fails. holderId={} messageId={}", holderId, messageRecord.uid);
        }
        else {
            log.info("sent. holderId={} messageId={} tranId={}", holderId, messageRecord.uid, id);
        }
    }

    private void checkUnconfirmedMessages() {
        List<MessageRecord> overdueMessages = new ArrayList<>();
        long thresholdTime = System.currentTimeMillis() - confirmTimeout;
        synchronized (unconfirmedMessages) {
            for(Map.Entry<String, MessageRecord> statusEntry : unconfirmedMessages.entrySet()) {
                if (statusEntry.getValue().timestamp < thresholdTime) {
                    log.warn("checkUnconfirmedMessages. Message {} was sent at {} - resend.",
                        statusEntry.getKey(), new Instant(statusEntry.getValue().timestamp));
                    overdueMessages.add(statusEntry.getValue());
                }
            }
        }
        for(MessageRecord messageRecord : overdueMessages) {
            send(messageRecord);
        }
        log.debug("checkUnconfirmedMessages. {} messages resent. Total sent {} received {}, self received {}, send try {}", 
            overdueMessages.size(), sentMessageCount.get(), receivedMessageCount.get(), receivedSelfMessageCount.get(), sentMessageTryCount.get());
    }

    private void sendMessage(Consumer<NetworkMessagesReceiver> action) {
        for (NetworkMessagesReceiver messagesReceiver : messageReceivers) {
            try {
                action.accept(messagesReceiver);
            } catch (Exception e) {
                log.error(String.format("sendMessage fails. holderId=%s", holderId), e);
            }
        }
    }
    
    public void handleNewMessage(MessageContent messageContent, String msgId, boolean isCommitted, String authorId) {
        receivedMessageCount.incrementAndGet();
        String body = messageContent.getField(FIELD_BODY);
        String type = messageContent.getType();
        boolean isSelf = holderId.equals(messageContent.getAuthor());
        String messageId = messageContent.getUID();
        log.debug("handleNewMessage. message type={}. holderId={} authorId={}  messageId={} tranId={}", type, holderId, authorId, messageId, msgId);
        synchronized (unconfirmedMessages) {
            if (unconfirmedMessages.remove(messageId) != null) {
                receivedSelfMessageCount.incrementAndGet();
            }
        }
        try {
            switch (type) {
                case TYPE_VOTE:
                    if (holderId.equals(MasterNode.MASTER_HOLDER_ID)) {
                        addVoteToMaster(messageId, body, isCommitted, isSelf);
                    }
                    sendMessage(r -> r.notifyVote(messageId, isCommitted, isSelf));
                    break;
                case TYPE_VOTE_STATUS:
                    VoteStatus status = serializer.deserializeVoteStatus(body);
                    sendMessage(r -> r.addVoteStatus(status, messageId, isCommitted, isSelf));
                    break;
                case TYPE_VOTING:
                    if (!masterId.equals(messageContent.getAuthor())) {
                        log.error("TYPE_VOTING message {} author {} is not master {}. holderId={}", messageId, messageContent.getAuthor(), masterId, holderId);
                        break;
                    }
                    Voting voting = serializer.deserializeVoting(body);
                    sendMessage(r -> r.addVoting(voting));
                    break;
                case TYPE_VOTING_TOTAL_RESULT:
                    if (!masterId.equals(messageContent.getAuthor())) {
                        log.error("TYPE_VOTING_TOTAL_RESULT message {} author {} is not master {}. holderId={}", messageId, messageContent.getAuthor(), masterId, holderId);
                        break;
                    }
                    VoteResult result = serializer.deserializeVoteResult(body);
                    sendMessage(r -> r.addVotingTotalResult(result));
                    break;
                default:
                    log.warn("handleNewMessage. Unknown message type: {} messageId={} holderId={}", type, messageId, holderId);
            }
        } catch (InternalLogicException e) {
            log.error("handleNewMessage fails. message type={} messageId={} holderId={} error={}", type, messageId, holderId, e.getMessage());
        }
    }

    private void addVoteToMaster(String messageId, String body, boolean isCommitted, boolean isSelf) {
        try{
            String decryptedBody;
            try {
                decryptedBody = cryptoHelper.decrypt(body, privateKey);
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                log.error("handleVote. Undecrypted VOTE message {}. holderId={}", messageId, holderId);
                return;
            }
            String[] messageParts = MessageBuilder.splitMessage(decryptedBody);
            if (messageParts.length != 2) {
                log.error("handleVote. VOTE message {} has invalid number of parts {}. holderId={} decryptedBody={}", messageId, messageParts.length, holderId, decryptedBody);
                return;
            }
            VoteResult result;
            try {
                result = serializer.deserializeVoteResult(messageParts[0]);
            } catch (InternalLogicException e) {
                log.error("handleVote fails. deserializeVoteResult fails. messageId={} holderId={} error={}", messageId, holderId, e.getMessage());
                return;
            }
            try {
                if (!messageParts[1].equals(AssetsHolder.EMPTY_SIGNATURE) &&
                    !cryptoHelper.verifySignature(messageParts[0], messageParts[1], participantKeysById.get(result.getHolderId()))) {
                    log.error("handleVote. VOTE message {} has invalid owner signature. holderId={} result={} decryptedBody={}", messageId, holderId, result, decryptedBody);
                    return;
                }
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                log.error("handleVote fails. messageId={} holderId={} error={}", messageId, holderId, e.getMessage());
            }
            sendMessage(r -> r.addVoteToMaster(result, messageId, messageParts[0], isCommitted, isSelf));
            log.debug("VOTE handled  messageId={} holderId={}", messageId, holderId);
        } catch (Exception e) {
            log.error(String.format("handleVote fails. messageId=%s holderId=%s", messageId, holderId), e);
        }
    }
}
