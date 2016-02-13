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

package uk.dsxt.voting.resultsbuilder;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.datamodel.VoteResult;
import uk.dsxt.voting.common.networking.ResultsBuilder;

import java.util.HashMap;
import java.util.Map;

@Log4j2
public class ResultsManager implements ResultsBuilder {

    private final Map<String, Map<String, VoteResult>> resultsByHolderIdAndVotingId = new HashMap<>();

    private final Map<String, VoteResult> referenceResultsByVotingId = new HashMap<>();

    public void addResult(String holderId, String voteResult) {
        VoteResult result = new VoteResult(voteResult);
        synchronized (resultsByHolderIdAndVotingId) {
            Map<String, VoteResult> votingResults = resultsByHolderIdAndVotingId.get(result.getVotingId());
            if (votingResults == null) {
                votingResults = new HashMap<>();
                resultsByHolderIdAndVotingId.put(result.getVotingId(), votingResults);
            }
            votingResults.put(holderId, result);
        }
    }

    public void addVote(String voteResult) {
        VoteResult result = new VoteResult(voteResult);
        synchronized (referenceResultsByVotingId) {
            VoteResult referenceResult = referenceResultsByVotingId.get(result.getVotingId());
            if (referenceResult == null) {
                referenceResultsByVotingId.put(result.getVotingId(), result);
            } else {
                referenceResult.add(result);
            }
        }
    }

    public void checkVoting(String votingId) {
        synchronized (referenceResultsByVotingId) {
            VoteResult referenceResult = referenceResultsByVotingId.get(votingId);
            if (referenceResult == null) {
                referenceResult = new VoteResult(votingId, null);
                log.info("Empty result on voting {}", votingId);
            } else {
                log.info("Result on voting {} : {}", votingId, referenceResult);
            }

            synchronized (resultsByHolderIdAndVotingId) {
                Map<String, VoteResult> votingResults = resultsByHolderIdAndVotingId.get(votingId);
                if (votingResults == null) {
                    log.info("  No node results received on voting {}", votingId);
                } else {
                    log.info("  Received {} node results on voting {}", votingResults.size(), votingId);
                    for(Map.Entry<String, VoteResult> holderRecord : votingResults.entrySet()) {
                        if (!referenceResult.equals(holderRecord.getValue())) {
                            log.info("    Node result of holder {} on voting {} is not equal reference result: {}", holderRecord.getKey(), votingId, holderRecord.getValue());
                        }
                    }
                }
            }
        }

    }
}
