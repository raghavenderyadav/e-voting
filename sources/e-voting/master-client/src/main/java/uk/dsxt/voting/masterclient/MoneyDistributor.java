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

package uk.dsxt.voting.masterclient;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.datamodel.Participant;
import uk.dsxt.voting.common.datamodel.VoteResult;
import uk.dsxt.voting.common.datamodel.Voting;
import uk.dsxt.voting.common.networking.*;

import java.util.Timer;
import java.util.TimerTask;

@Log4j2
public class MoneyDistributor extends VotingClient {

    private final Timer sendResultTimer = new Timer("sendResultTimer");

    public MoneyDistributor(WalletManager walletManager, Participant[] participants,
                            ResultsBuilder resultsBuilder, VoteAggregation voteAggregation) {
        super(walletManager, voteAggregation, resultsBuilder, "master", null, participants);
    }

    public void addVoting(final Voting voting) {
        sendResultTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                VoteResult result = voteAggregation.getResult(voting.getId());
                if (result == null) {
                    result = new VoteResult(voting.getId(), null);
                }
                resultsBuilder.addResult("master", result.toString());
            }
        }, voting.getEndTimestamp() - System.currentTimeMillis());
    }

    @Override
    protected void handleNewMessage(MessageContent messageContent, String messageId, boolean isCommited) {
        if (MessageContent.TYPE_VOTE_RESULT.equals(messageContent.getType())) {
            super.handleNewMessage(messageContent, messageId, isCommited);
        }
    }
}
