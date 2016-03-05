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

package uk.dsxt.voting.common.cryptoVote;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.nodes.ClientNode;
import uk.dsxt.voting.common.messaging.MessagesSerializer;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;
import uk.dsxt.voting.common.utils.InternalLogicException;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
public class CryptoNodeDecorator implements CryptoVoteAcceptor {

    private final ClientNode node;

    private final CryptoVoteAcceptor parentNode;

    private final Map<String, Participant> participantsById;

    private final PrivateKey privateKey;

    private final MessagesSerializer serializer;

    private final CryptoHelper cryptoHelper;

    public CryptoNodeDecorator(ClientNode node, CryptoVoteAcceptor parentNode, MessagesSerializer serializer, CryptoHelper cryptoProvider, Map<String, Participant> participantsById, PrivateKey privateKey) {
        this.node = node;
        this.parentNode = parentNode;
        this.privateKey = privateKey;
        this.serializer = serializer;
        this.cryptoHelper = cryptoProvider;
        this.participantsById = participantsById;
        node.setParentHolder((newResult, signatures) -> acceptVote(newResult, signatures));
    }

    @Override
    public void acceptVote(String newResultMessage, String clientId, List<String> signatures)
            throws InternalLogicException, GeneralSecurityException, UnsupportedEncodingException {
        Participant participant = participantsById.get(clientId);
        if (participant == null)
            throw new InternalLogicException(String.format("Participant %s not found", clientId));
        if (signatures == null || signatures.size() == 0)
            throw new InternalLogicException(String.format("List of signatures is empty"));
        if (!cryptoHelper.verifySignature(newResultMessage, signatures.get(0), cryptoHelper.loadPublicKey(participant.getPublicKey())))
            throw new InternalLogicException(String.format("Participant %s signature is invalid", clientId));
        VoteResult newResult = serializer.deserializeVoteResult(newResultMessage);
        node.acceptVote(newResult, signatures);
    }

    private boolean acceptVote(VoteResult newResult, List<String> signatures) {
        String newResultMessage = serializer.serialize(newResult);
        try {
            String signature = cryptoHelper.createSignature(newResultMessage, privateKey);
            if (signatures == null)
                signatures = new ArrayList<>();
            signatures.add(0, signature);
            parentNode.acceptVote(newResultMessage, node.getParticipantId(), signatures);
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
