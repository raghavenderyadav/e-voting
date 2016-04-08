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

package uk.dsxt.voting.common.domain.nodes;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.domain.dataModel.*;
import uk.dsxt.voting.common.messaging.MessagesSerializer;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.MessageBuilder;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2
public class MasterNode extends ClientNode {

    public static String MASTER_HOLDER_ID = "00";
    
    private final ExecutorService handleVoteExecutor = Executors.newFixedThreadPool(10);

    public MasterNode(MessagesSerializer messagesSerializer, CryptoHelper cryptoProvider, Map<String, Participant> participantsById, PrivateKey privateKey) 
            throws InternalLogicException, GeneralSecurityException {
        super(MASTER_HOLDER_ID, messagesSerializer, cryptoProvider, participantsById, privateKey, null, null, null);
        parentHolder = new VoteChecker();
    }
    
    private class VoteChecker implements VoteAcceptor {

        @Override
        public NodeVoteReceipt acceptVote(String transactionId, String votingId, BigDecimal packetSize, String clientId, BigDecimal clientPacketResidual, String encryptedData, String voteDigest, String clientSignature) throws InternalLogicException {
            handleVoteExecutor.submit(() -> handleVote(transactionId, votingId, packetSize, clientId, clientPacketResidual, encryptedData, voteDigest));
            return null; 
        }
    }
    
    private void handleVote(String transactionId, String votingId, BigDecimal packetSize, String clientId, BigDecimal clientPacketResidual, String encryptedData, String voteDigest) {
        VoteResultStatus status = null;
        try {
            status = handleVote(transactionId, votingId, encryptedData, voteDigest);
        } catch (InternalLogicException e) {
            status = VoteResultStatus.InternalError;
            log.error("handleVote failed: {}", e.getMessage());
        }
        if (status != VoteResultStatus.OK) {
            try {
                network.addVoteStatus(new VoteStatus(votingId, transactionId, status, voteDigest, AssetsHolder.EMPTY_SIGNATURE));
            } catch (InternalLogicException e) {
                log.error("handleVote. addVoteStatus failed: {}", e.getMessage());
            }
        }
        /*
        String inputMessage = buildMessage(transactionId, votingId, packetSize, clientId, clientPacketResidual, encryptedData, voteDigest);
        long now = System.currentTimeMillis();
        String signedText = MessageBuilder.buildMessage(inputMessage, Long.toString(now), status.toString());
        String receiptSign;
        try {
            receiptSign = cryptoHelper.createSignature(signedText, privateKey);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            throw new InternalLogicException("Can not sign vote");
        }
        return new NodeVoteReceipt(inputMessage, now, status, receiptSign);
        */
    }
    
    private VoteResultStatus handleVote(String transactionId, String votingId, String encryptedData, String voteDigest) throws InternalLogicException {
        while (true) {
            String decryptedData;
            try {
                decryptedData = cryptoHelper.decrypt(encryptedData, privateKey);
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                log.error("handleVote. Failed to decrypt data. transactionId={} error={}", transactionId, e.getMessage());
                return VoteResultStatus.SignatureFailed;
            }
            String[] decryptedParts = MessageBuilder.splitMessage(decryptedData);
            if (decryptedParts.length == 2) {
                String sign = decryptedParts[1];
                if (!sign.equals(AssetsHolder.EMPTY_SIGNATURE)) {
                    Participant participant = participantsById.get(decryptedParts[0]);
                    if (participant == null) {
                        log.error("handleVote. Owner {} not found. transactionId={}", decryptedParts[0], transactionId);
                        return VoteResultStatus.IncorrectMessage;
                    }
                    if (participant.getPublicKey() == null) {
                        log.error("handleVote. Owner {} has no public key. transactionId={}", decryptedParts[0], transactionId);
                        return VoteResultStatus.IncorrectMessage;
                    }
                    try {
                        if (!cryptoHelper.verifySignature(decryptedParts[0], sign, cryptoHelper.loadPublicKey(participant.getPublicKey()))) {
                            log.error("handleVote. Invalid signature of owner {}. transactionId={}", decryptedParts[0], transactionId);
                            return VoteResultStatus.SignatureFailed;
                        }
                    } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                        log.error("handleVote. Failed to check owner signature. participantId={} transactionId={} error={}", decryptedParts[0], transactionId, e.getMessage());
                        return VoteResultStatus.SignatureFailed;
                    }
                }
                String resultSign;
                try {
                    resultSign = cryptoHelper.createSignature(decryptedParts[0], privateKey);
                } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                    log.error("handleVote. Failed to create signature owner signature. transactionId={} error={}", transactionId, e.getMessage());
                    return VoteResultStatus.InternalError;
                }
                network.addVoteStatus(new VoteStatus(votingId, transactionId, VoteResultStatus.OK, voteDigest, resultSign));
                return VoteResultStatus.OK;
            } else if (decryptedParts.length == 3) {
                encryptedData = decryptedParts[0];
                String participantId = decryptedParts[1];
                String sign = decryptedParts[2];
                Participant participant = participantsById.get(participantId);
                if (participant == null) {
                    log.error("handleVote. Participant {} not found. transactionId={}", participantId, transactionId);
                    return VoteResultStatus.IncorrectMessage;
                }
                if (participant.getPublicKey() == null) {
                    log.error("handleVote. Participant {} has no public key. transactionId={}", participantId, transactionId);
                    return VoteResultStatus.IncorrectMessage;
                }
                try {
                    if (!cryptoHelper.verifySignature(MessageBuilder.buildMessage(encryptedData, participantId), sign, cryptoHelper.loadPublicKey(participant.getPublicKey()))) {
                        log.error("handleVote. Invalid signature of participant {}. transactionId={}", participantId, transactionId);
                        return VoteResultStatus.SignatureFailed;
                    }
                } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                    log.error("handleVote. Failed to check participant signature. participantId={}. transactionId={} error={}", participantId, transactionId, e.getMessage());
                    return VoteResultStatus.SignatureFailed;
                }
            } else {
                log.error("handleVote. decryptedData contains {}. transactionId={} parts.", transactionId, decryptedParts.length);
                return VoteResultStatus.IncorrectMessage;
            }
        }
    }
}
