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
import uk.dsxt.voting.common.domain.dataModel.Participant;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    private final List<NetworkMessagesReceiver> messageReceivers = new ArrayList<>();

    private final MessagesSerializer serializer;

    private final CryptoHelper cryptoHelper;

    private final Map<String, Participant> participantsById;

    private final PrivateKey privateKey;

    private final String holderId;

    private final String masterId;
    
    private final ExecutorService voteMessagesExecutor = Executors.newFixedThreadPool(10);

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
    public String addVote(VoteResult result, String serializedVote, String ownerSignature, String nodeSignature) {
        Participant participant = participantsById.get(masterId);
        if (participant == null) {
            log.error("addVote. master node {} not found holderId={}", masterId, holderId);
            return null;
        }
        if (participant.getPublicKey() == null) {
            log.error("addVote. master node {} does not have public key holderId={}", masterId, holderId);
            return null;
        }
        String message = MessageBuilder.buildMessage(serializedVote, ownerSignature, nodeSignature);
        String encryptedMessage;
        try {
            encryptedMessage = cryptoHelper.encrypt(message, cryptoHelper.loadPublicKey(participant.getPublicKey()));
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            log.error("addVote. can not encrypt message. receiverId={}. error={} holderId={}", masterId, e.getMessage(), holderId);
            return null;
        }
        return send(TYPE_VOTE, encryptedMessage);
    }

    private String send(String messageType, String messageBody) {
        Map<String, String> fields = new HashMap<>();
        fields.put(FIELD_BODY, messageBody);
        try {
            String id = walletManager.sendMessage(MessageContent.buildOutputMessage(messageType, holderId, privateKey, cryptoHelper, fields));
            if (id == null) {
                log.error("send {} fails. holderId={}", messageType, holderId);
                return null;
            }
            else
                log.info("{} sent. holderId={} id={}", messageType, holderId, id);
            return id;
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            log.error("send {} fails: {}. holderId={}", messageType, e.getMessage(), holderId);
            return null;
        }
    }

    private void sendMessage(Consumer<NetworkMessagesReceiver> action) {
        for (NetworkMessagesReceiver messagesReceiver : messageReceivers) {
            try {
                action.accept(messagesReceiver);
            } catch (Exception e) {
                log.error("sendMessage fails. holderId={}", holderId, e);
            }
        }
    }

    public void handleNewMessage(MessageContent messageContent, String messageId, boolean isCommitted) {
        String body = messageContent.getField(FIELD_BODY);
        String type = messageContent.getType();
        log.debug("handleNewMessage. message type={} messageId={}. holderId={}", type, messageId, holderId);
        try {
            switch (type) {
                case TYPE_VOTE:
                    voteMessagesExecutor.execute(() -> handleVote(messageContent, messageId, body, isCommitted));
                    break;
                case TYPE_VOTE_STATUS:
                    VoteStatus status = serializer.deserializeVoteStatus(body);
                    sendMessage(r -> r.addVoteStatus(status, messageId, isCommitted));
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
            log.error("handleNewMessage fails. message type={} messageId={} holderId={}", type, messageId, holderId, e);
        }
    }

    private void handleVote(MessageContent messageContent, String messageId, String body, boolean isCommitted) {
        try{
            if (holderId.equals(MasterNode.MASTER_HOLDER_ID)) {
                String decryptedBody;
                try {
                    decryptedBody = cryptoHelper.decrypt(body, privateKey);
                } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                    log.error("handleVote. Undecrypted VOTE message {}. holderId={}", messageId, holderId);
                    return;
                }
                String[] messageParts = MessageBuilder.splitMessage(decryptedBody);
                if (messageParts.length != 3) {
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
                        !cryptoHelper.verifySignature(messageParts[0], messageParts[1], cryptoHelper.loadPublicKey(participantsById.get(result.getHolderId()).getPublicKey()))) {
                        log.error("handleVote. VOTE message {} has invalid owner signature. holderId={} result={} decryptedBody={}", messageId, holderId, result, decryptedBody);
                        return;
                    }
                    if (!cryptoHelper.verifySignature(messageParts[0], messageParts[2], cryptoHelper.loadPublicKey(participantsById.get(messageContent.getAuthor()).getPublicKey()))) {
                        log.error("handleVote. VOTE message {} has invalid node signature. holderId={} result={} decryptedBody={}", messageId, holderId, result, decryptedBody);
                        return;
                    }
                } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                    log.error("handleVote fails. messageId={} holderId={} error={}", messageId, holderId, e.getMessage());
                }
                sendMessage(r -> r.addVote(result, messageId, messageParts[0], isCommitted));
                log.debug("VOTE handled  messageId={} holderId={}", messageId, holderId);
            }
        } catch (Exception e) {
            log.error("handleVote fails. messageId={} holderId={}", messageId, holderId, e);
        }
    }
}
