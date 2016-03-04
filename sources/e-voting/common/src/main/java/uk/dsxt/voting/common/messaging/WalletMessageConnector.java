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

package uk.dsxt.voting.common.messaging;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.domain.nodes.NetworkMessagesReceiver;
import uk.dsxt.voting.common.domain.nodes.NetworkMessagesSender;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;
import uk.dsxt.voting.common.utils.InternalLogicException;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Log4j2
public class WalletMessageConnector implements NetworkMessagesSender {

    public static final String TYPE_VOTE = "VOTE";
    public static final String TYPE_VOTING = "VOTING";
    public static final String TYPE_VOTING_TOTAL_RESULT = "VOTING_TOTAL_RESULT";

    public static final String FIELD_BODY = "BODY";
    public static final String FIELD_VOTE_SIGNATURE = "VOTE_SIGNATURE";

    private final WalletManager walletManager;

    private final NetworkMessagesReceiver messageReceiver;

    private final MessagesSerializer serializer;

    private final CryptoHelper cryptoHelper;

    private final Map<String, Participant> participantsById;

    private final PrivateKey privateKey;

    private final String holderId;

    private final String masterId;

    @Override
    public void addVoting(Voting voting) {
        send(TYPE_VOTING, serializer.serialize(voting));
    }

    @Override
    public void addVotingTotalResult(VoteResult result) {
        send(TYPE_VOTING_TOTAL_RESULT, serializer.serialize(result));
    }

    @Override
    public void addVote(VoteResult result, String signature, String receiverId) {
        String resultMessage = serializer.serialize(result);
        Participant participant = participantsById.get(receiverId);
        if (participant == null) {
            log.error("addVote. receiver {} not found");
            return;
        }
        if (participant.getPublicKey() == null) {
            log.error("addVote. receiver {} does not have public key");
            return;
        }
        String encryptedMessage;
        try {
            encryptedMessage = cryptoHelper.encrypt(resultMessage, cryptoHelper.loadPublicKey(participant.getPublicKey()));
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            log.error("addVote. can not encrypt message. receiverId={}", receiverId);
            return;
        }
        Map<String, String> fields = new HashMap<>();
        fields.put(FIELD_BODY, encryptedMessage);
        fields.put(FIELD_VOTE_SIGNATURE, signature);
        send(TYPE_VOTE, fields);
    }

    private void send(String messageType, String messageBody) {
        Map<String, String> fields = new HashMap<>();
        fields.put(FIELD_BODY, messageBody);
        send(messageType, fields);
    }

    private void send(String messageType, Map<String, String> fields) {
        try {
            String id = walletManager.sendMessage(MessageContent.buildOutputMessage(messageType, holderId, privateKey, cryptoHelper, fields));
            if (id == null)
                log.error("send {} fails", messageType);
            else
                log.info("{} sent", messageType);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            log.error("send {} fails: {}", messageType, e.getMessage());
        }
    }

    public void handleNewMessage(MessageContent messageContent, String messageId) {
        if (messageReceiver == null)
            return;
        if (masterId.equals(messageContent.getAuthor())) {
            log.error("message {} author {} is not master {}", messageId, messageContent.getAuthor(), masterId);
            return;
        }
        String body = messageContent.getField(FIELD_BODY);
        String type = messageContent.getType();
        log.debug("handleNewMessage. message type={} messageId={}", type, messageId);
        try {
            switch (type) {
                case TYPE_VOTE:
                    String decryptedBody;
                    try {
                        decryptedBody = cryptoHelper.decrypt(body, privateKey);
                    } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                        log.debug("Undecrypted message {}", messageId);
                        break;
                    }
                    String signature = messageContent.getField(FIELD_VOTE_SIGNATURE);
                    if (signature == null) {
                        log.error("VOTE message {} without signature", messageId);
                        break;
                    }
                    if (signature.equals(cryptoHelper.createSignature(decryptedBody, privateKey))) {
                        log.error("VOTE message {} has invalid signature", messageId);
                        break;
                    }
                    messageReceiver.addVote(serializer.deserializeVoteResult(decryptedBody));
                    break;
                case TYPE_VOTING:
                    messageReceiver.addVoting(serializer.deserializeVoting(body));
                    break;
                case TYPE_VOTING_TOTAL_RESULT:
                    messageReceiver.addVotingTotalResult(serializer.deserializeVoteResult(body));
                    break;
                default:
                    log.warn("handleNewMessage. Unknown message type: {} messageId={}", type, messageId);
            }
        } catch (GeneralSecurityException | UnsupportedEncodingException | InternalLogicException e) {
            log.error("handleNewMessage fails. message type={} messageId={}", type, messageId, e);
        }
    }
}
