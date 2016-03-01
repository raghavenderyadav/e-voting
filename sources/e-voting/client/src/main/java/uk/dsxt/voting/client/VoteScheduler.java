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

import lombok.Value;
import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.datamodel.VoteResult;
import uk.dsxt.voting.common.datamodel.Voting;
import uk.dsxt.voting.common.networking.ResultsBuilder;
import uk.dsxt.voting.common.networking.VoteAggregation;
import uk.dsxt.voting.common.networking.VotingClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Log4j2
public class VoteScheduler {

    @Value
    private static class VoteRecord {
        long sendTimestamp;
        boolean sendToResult;
        VoteResult voteResult;
    }

    private final VotingClient votingClient;
    private final ResultsBuilder resultsBuilder;
    private final VoteAggregation aggregation;

    private final List<VoteRecord> recordsByTime = new ArrayList<>();
    private final String holderId;
    private int currentRecordIdx = 0;

    private Thread scheduler;

    public VoteScheduler(VotingClient votingClient, ResultsBuilder resultsBuilder, VoteAggregation aggregation, Voting[] votings,
                         String messagesFileContent, String holderId) {
        this.votingClient = votingClient;
        this.resultsBuilder = resultsBuilder;
        this.aggregation = aggregation;
        this.holderId = holderId;

        if (messagesFileContent == null) {
            log.info("messagesFile not found");
            return;
        }

        HashMap<String, Voting> votingsById = new HashMap<>();
        for (Voting voting : votings) {
            votingsById.put(voting.getId(), voting);
            recordsByTime.add(new VoteRecord(voting.getEndTimestamp(), true, new VoteResult(voting.getId(), null)));
        }

        String[] lines = messagesFileContent.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#"))
                continue;
            String[] votes = line.split(";");
            for(String vote : votes) {
                String[] terms = vote.split(":");
                if (terms.length < 2 || terms.length > 3)
                    throw new IllegalArgumentException(String.format("Vote schedule record can not be created from string with %d terms (%s)", terms.length, line));
                VoteResult voteResult = new VoteResult(terms[1]);
                Voting voting = votingsById.get(voteResult.getVotingId());
                if (voting == null) {
                    log.error("Can not find voting with id {}", voteResult.getVotingId());
                    continue;
                }
                long sendTimestamp = voting.getStartTimestamp() + Integer.parseInt(terms[0]) * 60 * 1000;
                boolean sendToResult = terms.length < 3 || !terms[2].equals("-");
                recordsByTime.add(new VoteRecord(sendTimestamp, sendToResult, voteResult));
            }
        }
        recordsByTime.sort((x, y) -> Long.compare(x.getSendTimestamp(), y.getSendTimestamp()));
        log.info("{} vote records loaded", recordsByTime.size());
    }

    public void run() {
        scheduler = new Thread(this::sendVotesOnTime, "VoteScheduler");
        scheduler.start();
        log.info("VoteScheduler #{} runs", holderId);
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.interrupt();
            scheduler = null;
        }
    }

    private void sendVotesOnTime() {
        while (!Thread.currentThread().isInterrupted() && currentRecordIdx < recordsByTime.size()) {
            for (; currentRecordIdx < recordsByTime.size() && recordsByTime.get(currentRecordIdx).getSendTimestamp() < System.currentTimeMillis() + 1000; currentRecordIdx++) {
                VoteResult voteResult = recordsByTime.get(currentRecordIdx).getVoteResult();
                if (voteResult.getHolderId() != null) {
                    log.info("Sending vote record {}", voteResult);
                    if (votingClient.sendVoteResult(voteResult) && recordsByTime.get(currentRecordIdx).isSendToResult()) {
                        resultsBuilder.addVote(voteResult.toString());
                    }
                } else {
                    VoteResult result = aggregation.getResult(voteResult.getVotingId());
                    if (result == null) {
                        result = new VoteResult(voteResult.getVotingId(), null);
                    }
                    resultsBuilder.addResult(holderId, result.toString());
                }
            }
            if (currentRecordIdx < recordsByTime.size()) {
                try {
                    Thread.sleep(recordsByTime.get(currentRecordIdx).getSendTimestamp() - System.currentTimeMillis());
                } catch (InterruptedException e) {
                    log.info("sendVotesOnTime interrupted");
                    return;
                }
            }
        }
        if (!Thread.currentThread().isInterrupted())
            log.info("All vote records sent");
    }

}
