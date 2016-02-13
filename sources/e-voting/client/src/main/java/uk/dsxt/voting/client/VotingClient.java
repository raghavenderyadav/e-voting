/******************************************************************************
 * e-voting system                                                            *
 * Copyright (C) 2016 DSX Technologies Limited.                               *
 *                                                                            *
 * This program is free software; you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation; either version 2 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied                         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * You can find copy of the GNU General Public License in LICENSE.txt file    *
 * at the top-level directory of this distribution.                           *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package uk.dsxt.voting.client;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.datamodel.Participant;
import uk.dsxt.voting.common.networking.MessageContent;
import uk.dsxt.voting.common.networking.MessageHandler;
import uk.dsxt.voting.common.networking.WalletManager;

import java.security.PrivateKey;
import java.util.*;

@Log4j2
public class VotingClient extends MessageHandler {

    private final VoteAggregation voteAggregation;

    private final String ownerId;

    private final PrivateKey ownerPrivateKey;

    private final List<VoteResult> sentVoteResults = new ArrayList<>();

    public VotingClient(WalletManager walletManager, VoteAggregation voteAggregation, String ownerId, PrivateKey ownerPrivateKey, Participant[] participants) {
        super(walletManager, participants);
        this.voteAggregation = voteAggregation;
        this.ownerId = ownerId;
        this.ownerPrivateKey = ownerPrivateKey;

        if (walletManager.getBalance().signum() == 0) {
            sendInitialMoneyRequest();
        }
    }

    private void sendInitialMoneyRequest() {
        Map<String, String> fields = new HashMap<>();
        fields.put(MessageContent.FIELD_WALLET, walletManager.getSelfAddress());
        try {
            walletManager.sendMessage(MessageContent.buildOutputMessage(MessageContent.TYPE_INITIAL_MONEY_REQUEST, ownerId, ownerPrivateKey, fields));
            log.info("initialMoneyRequest sent");
        } catch (Exception e) {
            log.error("sendInitialMoneyRequest fails", e);
        }
    }

    public void sendVoteResult(VoteResult voteResult) {
        Map<String, String> fields = new HashMap<>();
        fields.put(MessageContent.FIELD_VOTE_RESULT, voteResult.toString());
        try {
            walletManager.sendMessage(MessageContent.buildOutputMessage(MessageContent.TYPE_VOTE_RESULT, ownerId, ownerPrivateKey, fields));
            sentVoteResults.add(voteResult);
            log.info("voteResult sent");
        } catch (Exception e) {
            log.error("sendVoteResult fails", e);
        }
    }

    @Override
    protected void handleNewMessage(MessageContent messageContent, String messageId) {
        if (MessageContent.TYPE_VOTE_RESULT.equals(messageContent.getType())) {
            log.info("Message {} contains vote result", messageId);
            VoteResult result = new VoteResult(messageContent.getField(MessageContent.FIELD_VOTE_RESULT));
            voteAggregation.addVote(result, messageContent.getFieldTimestamp(), messageContent.getAuthor());
        }
    }

}
