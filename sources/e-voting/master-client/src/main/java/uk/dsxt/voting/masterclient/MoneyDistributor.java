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

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

@Log4j2
public class MoneyDistributor extends VotingClient {

    private final BigDecimal moneyToNode;

    private final Set<String> sentIds = new HashSet<>();

    private final Timer sendResultTimer = new Timer("sendResultTimer");

    public MoneyDistributor(WalletManager walletManager, Participant[] participants, BigDecimal moneyToNode, Voting[] votings,
                            ResultsBuilder resultsBuilder, VoteAggregation voteAggregation, PrivateKey ownerPrivateKey, String ownerId) {
        super(walletManager, voteAggregation, resultsBuilder, ownerId, ownerPrivateKey, votings, participants);
        this.moneyToNode = moneyToNode;

        final String[] ids = new String[1];
        for (Voting v : votingsById.values()) {
            ids[0] = v.getId();
            sendResultTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    VoteResult result = voteAggregation.getResult(ids[0]);
                    if (result == null) {
                        result = new VoteResult(ids[0], null);
                    }
                    resultsBuilder.addResult("master", result.toString());
                }
            }, v.getEndTimestamp() - System.currentTimeMillis());
        }
    }

    @Override
    protected void handleNewMessage(MessageContent messageContent, String messageId, boolean isCommited) {
        if (MessageContent.TYPE_INITIAL_MONEY_REQUEST.equals(messageContent.getType())) {
            log.info("Message {} contains initial money request", messageId);
            if (sentIds.contains(messageContent.getAuthor())) {
                log.warn("Message {} author {} already has money", messageId, messageContent.getAuthor());
                return;
            }
            String wallet = messageContent.getField(MessageContent.FIELD_WALLET);
            walletManager.sendMoneyToAddressBalance(moneyToNode, wallet);
            log.info("{} money sent to {}", moneyToNode, wallet);
            sentIds.add(messageContent.getAuthor());
        } else if (MessageContent.TYPE_VOTE_RESULT.equals(messageContent.getType()))
            super.handleNewMessage(messageContent, messageId, isCommited);
    }
}
