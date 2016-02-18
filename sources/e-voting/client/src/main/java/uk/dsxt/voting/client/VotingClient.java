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
import uk.dsxt.voting.common.datamodel.VoteResult;
import uk.dsxt.voting.common.datamodel.Voting;
import uk.dsxt.voting.common.networking.MessageContent;
import uk.dsxt.voting.common.networking.MessageHandler;
import uk.dsxt.voting.common.networking.ResultsBuilder;
import uk.dsxt.voting.common.networking.WalletManager;

import java.security.PrivateKey;
import java.util.*;

@Log4j2
public class VotingClient extends MessageHandler {

    private final VoteAggregation voteAggregation;

    private final ResultsBuilder resultsBuilder;

    private final String holderId;

    private final PrivateKey ownerPrivateKey;

    private final Map<String, Voting> votingsById = new HashMap<>();


    public VotingClient(WalletManager walletManager, VoteAggregation voteAggregation, ResultsBuilder resultsBuilder,
                        String holderId, PrivateKey ownerPrivateKey, Voting[] votings, Participant[] participants) {
        super(walletManager, participants);
        this.voteAggregation = voteAggregation;
        this.resultsBuilder = resultsBuilder;
        this.holderId = holderId;
        this.ownerPrivateKey = ownerPrivateKey;
        Arrays.stream(votings).forEach(v -> votingsById.put(v.getId(), v));
    }

    public boolean sendVoteResult(VoteResult voteResult) {
        Map<String, String> fields = new HashMap<>();
        fields.put(MessageContent.FIELD_VOTE_RESULT, voteResult.toString());
        try {
            String id = walletManager.sendMessage(MessageContent.buildOutputMessage(MessageContent.TYPE_VOTE_RESULT, holderId, ownerPrivateKey, fields));
            if (id == null) {
                log.error("sendVoteResult fails");
                return false;
            }
            log.info("voteResult sent");
            return true;
        } catch (Exception e) {
            log.error("sendVoteResult fails", e);
            return false;
        }
    }

    @Override
    protected void handleNewMessage(MessageContent messageContent, String messageId) {
        if (MessageContent.TYPE_VOTE_RESULT.equals(messageContent.getType())) {
            log.info("Message {} contains vote result", messageId);
            VoteResult result = new VoteResult(messageContent.getField(MessageContent.FIELD_VOTE_RESULT));
            Voting voting = votingsById.get(result.getVotingId());
            if (voting == null) {
                return;
            }
            if (voteAggregation.addVote(result, messageContent.getFieldTimestamp(), messageContent.getAuthor()) &&
                    voting.getEndTimestamp() >= System.currentTimeMillis())
                resultsBuilder.addResult(holderId, voteAggregation.getResult(result.getVotingId()).toString());
        }
    }

}
