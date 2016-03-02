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

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.domain.nodes.BroadcastingMessageConnector;
import uk.dsxt.voting.common.domain.nodes.ClientNode;
import uk.dsxt.voting.common.domain.nodes.VoteAcceptor;
import uk.dsxt.voting.common.utils.CryptoHelper;
import uk.dsxt.voting.common.utils.InternalLogicException;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class CryptoNodeDecorator implements CryptoVoteAcceptor {

    private static final String MESSAGE_PART_SEPARATOR = "|";

    private final ClientNode node;

    private CryptoVoteAcceptor parentNode;

    private final Map<String, Participant> participantsById;

    private PrivateKey privateKey;

    private MessagesSerializer serializer;

    public CryptoNodeDecorator(ClientNode node, CryptoVoteAcceptor parentNode, Participant[] participants, PrivateKey privateKey, MessagesSerializer serializer) {
        this.node = node;
        this.parentNode = parentNode;
        this.privateKey = privateKey;
        this.serializer = serializer;
        participantsById = Arrays.stream(participants).collect(Collectors.toMap(Participant::getId, Function.identity()));
        node.setParentHolder((newResult, clientId, holdersTreePath) -> acceptVote(newResult, clientId, holdersTreePath));
    }

    @Override
    public void acceptVote(String newResultMessage, String clientId, String holdersTreePath, String signature)
            throws InternalLogicException, GeneralSecurityException, UnsupportedEncodingException {
        Participant participant = participantsById.get(clientId);
        if (participant == null)
            throw new InternalLogicException(String.format("Participant %s not found", clientId));
        String fullMessageText = String.format("%s%s%s", newResultMessage, MESSAGE_PART_SEPARATOR, holdersTreePath);
        if (!CryptoHelper.verifySignature(fullMessageText, signature, CryptoHelper.loadPublicKey(participant.getPublicKey())))
            throw new InternalLogicException(String.format("Participant %s signature is invalid", clientId));
        VoteResult newResult = serializer.deserializeVoteResult(newResultMessage);
        node.acceptVote(newResult, clientId, holdersTreePath);
    }

    private boolean acceptVote(VoteResult newResult, String clientId, String holdersTreePath) {
        String newResultMessage = serializer.serialize(newResult);
        String fullMessageText = String.format("%s%s%s", newResultMessage, MESSAGE_PART_SEPARATOR, holdersTreePath);
        try {
            String signature = CryptoHelper.createSignature(fullMessageText, privateKey);
            parentNode.acceptVote(newResultMessage, clientId, holdersTreePath, signature);
            return true;
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            log.error("acceptVote. Can not create signature: {}", e.getMessage());
            return false;
        } catch (InternalLogicException e) {
            log.error("acceptVote. Can not send message to parent : {}", e.getMessage());
            return false;
        }
    }
}
