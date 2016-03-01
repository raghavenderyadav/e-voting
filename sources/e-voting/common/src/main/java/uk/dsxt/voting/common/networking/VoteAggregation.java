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
import uk.dsxt.voting.common.datamodel.Client;
import uk.dsxt.voting.common.datamodel.VoteResult;
import uk.dsxt.voting.common.datamodel.Voting;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class VoteAggregation {

    private final Map<String, VoteAggregator> aggregatorsByVotingId = new HashMap<>();
    private final Client[] clients;

    public VoteAggregation(Client[] clients) {
        this.clients = clients;
    }

    public void addVoting(Voting voting) {
        aggregatorsByVotingId.put(voting.getId(), new VoteAggregator(voting, clients));
    }

    public boolean addVote(VoteResult voteResult, long timestamp, String signAuthorId) {
        VoteAggregator aggregator = aggregatorsByVotingId.get(voteResult.getVotingId());
        if (aggregator == null) {
            log.warn("Can not add vote of holder {} to voting {}: voting not found",
                    voteResult.getHolderId(), voteResult.getVotingId());
            return false;
        } else {
            return aggregator.addVote(voteResult, timestamp, signAuthorId);
        }
    }

    @SuppressWarnings("unused")
    public VoteResult getResult(String votingId) {
        VoteAggregator aggregator = aggregatorsByVotingId.get(votingId);
        if (aggregator == null) {
            log.warn("Can not get voting {} results: voting not found", votingId);
            return null;
        } else {
            return aggregator.getResult();
        }
    }

}
