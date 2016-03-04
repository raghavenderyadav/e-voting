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

package uk.dsxt.voting.common.networking;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.demo.ResultsBuilder;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.messaging.MessageContent;
import uk.dsxt.voting.common.messaging.WalletManager;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;

import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class VotingClient extends MessageHandler {

    private static final String TYPE_VOTE_RESULT = "VOTE";
    private static final String FIELD_VOTE_RESULT = "RESULT";

    protected final VoteAggregation voteAggregation;

    protected final ResultsBuilder resultsBuilder;

    private final String holderId;

    private final PrivateKey ownerPrivateKey;

    protected final Map<String, Voting> votingsById = new HashMap<>();

    private final static CryptoHelper cryptoHelper = CryptoHelper.DEFAULT_CRYPTO_HELPER;

    public VotingClient(WalletManager walletManager, VoteAggregation voteAggregation, ResultsBuilder resultsBuilder,
                        String holderId, PrivateKey ownerPrivateKey, Participant[] participants) {
        super(walletManager, cryptoHelper, participants);
        this.voteAggregation = voteAggregation;
        this.resultsBuilder = resultsBuilder;
        this.holderId = holderId;
        this.ownerPrivateKey = ownerPrivateKey;
    }

    public void addVoting(Voting voting) {
        votingsById.put(voting.getId(), voting);
    }

    public boolean sendVoteResult(VoteResult voteResult) {
        Map<String, String> fields = new HashMap<>();
        fields.put(FIELD_VOTE_RESULT, voteResult.toString());
        try {
            String id = walletManager.sendMessage(MessageContent.buildOutputMessage(TYPE_VOTE_RESULT, holderId, ownerPrivateKey, cryptoHelper, fields));
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
        if (TYPE_VOTE_RESULT.equals(messageContent.getType())) {
            VoteResult result = new VoteResult(messageContent.getField(FIELD_VOTE_RESULT));
            log.info("Client {} receive message {} with vote result {}", holderId, messageId, result);
            Voting voting = votingsById.get(result.getVotingId());
            if (voting == null) {
                return;
            }
            if (voteAggregation.addVote(result, messageContent.getFieldTimestamp(), messageContent.getAuthor()) &&
                    voting.getEndTimestamp() <= System.currentTimeMillis())
                resultsBuilder.addResult(holderId, voteAggregation.getResult(result.getVotingId()).toString());
        }
    }

}
