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
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.VoteResultStatus;
import uk.dsxt.voting.common.domain.dataModel.VoteStatus;
import uk.dsxt.voting.common.messaging.MessagesSerializer;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Map;

@Log4j2
public class MasterNode extends ClientNode {

    public static String MASTER_HOLDER_ID = "00";

    public MasterNode(MessagesSerializer messagesSerializer, CryptoHelper cryptoProvider, Map<String, Participant> participantsById, PrivateKey privateKey) 
            throws InternalLogicException, GeneralSecurityException {
        super(MASTER_HOLDER_ID, messagesSerializer, cryptoProvider, participantsById, privateKey, null);
        parentHolder = new VoteChecker();
    }
    
    private class VoteChecker implements VoteAcceptor {

        @Override
        public VoteResultStatus acceptVote(String transactionId, String votingId, BigDecimal packetSize, String clientId, BigDecimal clientPacketResidual, String encryptedData, String clientSignature) {

            while (true) {
                String decryptedData;
                try {
                    decryptedData = cryptoHelper.decrypt(encryptedData, privateKey);
                } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                    log.error("VoteChecker.acceptVote. Failed to decrypt data. error={}", e.getMessage());
                    return VoteResultStatus.SignatureFailed;
                }
                String[] decryptedParts = splitMessage(decryptedData);
                if (decryptedParts.length == 2) {
                    String sign = decryptedParts[1];
                    if (!sign.equals(AssetsHolder.EMPTY_SIGNATURE)) {
                        VoteResult result;
                        try {
                            result = messagesSerializer.deserializeVoteResult(decryptedParts[0]);
                        } catch (InternalLogicException e) {
                            log.error("VoteChecker.acceptVote. Failed to deserializeVoteResult. error={}", e.getMessage());
                            return VoteResultStatus.IncorrectMessage;
                        }
                        Participant participant = participantsById.get(result.getHolderId());
                        if (participant == null) {
                            log.error("VoteChecker.acceptVote. Owner {} not found", result.getHolderId());
                            return VoteResultStatus.IncorrectMessage;
                        }
                        if (participant.getPublicKey() == null) {
                            log.error("VoteChecker.acceptVote. Owner {} has no public key", result.getHolderId());
                            return VoteResultStatus.IncorrectMessage;
                        }
                        try {
                            if (!cryptoHelper.verifySignature(decryptedParts[0], sign, cryptoHelper.loadPublicKey(participant.getPublicKey()))) {
                                log.error("VoteChecker.acceptVote. Invalid signature of owner {}", result.getHolderId());
                                return VoteResultStatus.SignatureFailed;
                            }
                        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                            log.error("VoteChecker.acceptVote. Failed to check owner signature. participantId={} error={}", result.getHolderId(), e.getMessage());
                            return VoteResultStatus.SignatureFailed;
                        }
                    }
                    String resultSign;
                    try {
                        resultSign = cryptoHelper.createSignature(decryptedParts[0], privateKey);
                    } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                        log.error("VoteChecker.acceptVote. Failed to create signature owner signature. transactionId={} error={}", transactionId, e.getMessage());
                        return VoteResultStatus.InternalError;
                    }
                    network.addVoteStatus(new VoteStatus(votingId, transactionId, VoteResultStatus.OK, resultSign));
                    return VoteResultStatus.OK;
                } else if (decryptedParts.length == 3) {
                    encryptedData = decryptedParts[0];
                    String participantId = decryptedParts[1];
                    String sign = decryptedParts[2];
                    Participant participant = participantsById.get(participantId);
                    if (participant == null) {
                        log.error("VoteChecker.acceptVote. Participant {} not found", participantId);
                        return VoteResultStatus.IncorrectMessage;
                    }
                    if (participant.getPublicKey() == null) {
                        log.error("VoteChecker.acceptVote. Participant {} has no public key", participantId);
                        return VoteResultStatus.IncorrectMessage;
                    }
                    try {
                        if (!cryptoHelper.verifySignature(buildMessage(encryptedData, participantId), sign, cryptoHelper.loadPublicKey(participant.getPublicKey()))) {
                            log.error("VoteChecker.acceptVote. Invalid signature of participant {}", participantId);
                            return VoteResultStatus.SignatureFailed;
                        }
                    } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                        log.error("VoteChecker.acceptVote. Failed to check participant signature. participantId={} error={}", participantId, e.getMessage());
                        return VoteResultStatus.SignatureFailed;
                    }
                } else {
                    log.error("VoteChecker.acceptVote. decryptedData contains {} parts.", decryptedParts.length);
                    return VoteResultStatus.IncorrectMessage;
                }
            }
        }
    }
}
