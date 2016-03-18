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

import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.VoteResultStatus;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.messaging.MessagesSerializer;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class MasterNode extends ClientNode {

    @Setter
    private NetworkMessagesSender network;

    private final ScheduledExecutorService calculateResultsService;

    public static String MASTER_HOLDER_ID = "00";

    public MasterNode(MessagesSerializer messagesSerializer) {
        super(MASTER_HOLDER_ID, messagesSerializer);
        calculateResultsService = Executors.newScheduledThreadPool(10);
    }

    public void addNewVoting(Voting voting) {
        network.addVoting(voting);
        calculateResultsService.schedule(() -> calculateResults(voting.getId()), Math.max(voting.getEndTimestamp() - System.currentTimeMillis(), 0) + 60000, TimeUnit.MILLISECONDS);
        log.info("Voting added. votingId={}", voting.getId());
    }

    private void calculateResults(String votingId) {
        log.info("calculateResults started. votingId={}", votingId);
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            log.warn("calculateResults. Voting not found {}", votingId);
            return;
        }
        VoteResult totalResult = new VoteResult(votingId, null);
        votingRecord.sumClientResultsByClientId.values().stream().filter(r -> r.getStatus() == VoteResultStatus.OK).forEach(r -> totalResult.add(r));
        log.info("calculateResults. totalResult={}", totalResult);
        //TODO: move to common logic
        try {
            VoteResult adaptedTotalResult = messagesSerializer.adaptVoteResultForXML(totalResult, votingRecord.voting);
            network.addVotingTotalResult(adaptedTotalResult);
        } catch (Exception e) {
            log.error("calculateResults failed", e);
        }
    }

    @Override
    public synchronized boolean acceptVote(VoteResult newResult, List<String> signatures) {
        if (super.acceptVote(newResult, signatures)) {
            String[] holderIds = newResult.getHolderId().split(ClientNode.PATH_SEPARATOR);
            String holderPath = null;
            if (signatures.size() < holderIds.length - 1 || signatures.size() > holderIds.length) {
                log.error("acceptVote.holderIds.length={} but signatures.size()={}", holderIds.length, signatures.size());
                return false;
            }
            for (int i = 0; i < holderIds.length; i++) {
                int idx = holderIds.length - i - 1;
                String holderId = holderIds[idx];
                holderPath = i == 0 ? holderId : holderId + ClientNode.PATH_SEPARATOR + holderPath;
                if (idx < signatures.size()) {
                    network.addVote(new VoteResult(newResult, holderPath), signatures.get(idx), holderId);
                }
            }
            return true;
        }
        return false;
    }
}
