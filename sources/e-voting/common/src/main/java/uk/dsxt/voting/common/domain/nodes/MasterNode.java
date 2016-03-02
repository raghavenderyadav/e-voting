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
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.VoteResultStatus;
import uk.dsxt.voting.common.domain.dataModel.Voting;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class MasterNode extends ClientNode {

    private final BroadcastingMessageConnector network;

    private final ScheduledExecutorService calculateResultsService;

    public MasterNode(BroadcastingMessageConnector network, Voting[] votings) {
        super("0");
        this.network = network;
        calculateResultsService = Executors.newScheduledThreadPool(10);
        for(Voting voting : votings) {
            network.addVoting(voting);
            calculateResultsService.schedule(() -> calculateResults(voting.getId()), voting.getEndTimestamp() - System.currentTimeMillis()+60000, TimeUnit.MILLISECONDS);
            log.info("Voting added. votingId={}", voting.getId());
        }
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
        network.addVotingTotalResult(totalResult);
    }


}
