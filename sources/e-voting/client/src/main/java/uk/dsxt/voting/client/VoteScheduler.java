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
import uk.dsxt.voting.common.datamodel.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Log4j2
public class VoteScheduler {

    @Value
    private static class VoteRecord {
        long sendTimestamp;
        VoteResult voteResult;
    }

    private VoitingClient voitingClient;

    private List<VoteRecord> recordsByTime = new ArrayList<>();
    private int currentRecordIdx = 0;

    public VoteScheduler(VoitingClient voitingClient, Voting[] votings, String messagesFileContent) {
        this.voitingClient = voitingClient;

        if (messagesFileContent == null) {
            log.info("messagesFile not found");
            return;
        }

        HashMap<String, Voting> votingsById = new HashMap<>();
        for(Voting voting : votings) {
            votingsById.put(voting.getId(), voting);
        }

        String[] lines = messagesFileContent.split("\\r?\\n");
        for(String line : lines) {
            line = line.trim();
            if (line.startsWith("#"))
                continue;
            String[] terms = line.split(":");
            if (terms.length != 2)
                throw new IllegalArgumentException(String.format("Vote schedule record can not be created from string with %d terms (%s)", terms.length, line));
            VoteResult voteResult = new VoteResult(terms[1]);
            Voting voting = votingsById.get(voteResult.getVotingId());
            if (voting == null) {
                log.error("Can not find voting with id {}", voteResult.getVotingId());
                continue;
            }
            long sendTimestamp = voting.getStartTimestamp() + Integer.parseInt(terms[0]) * 60 * 1000;
            recordsByTime.add(new VoteRecord(sendTimestamp, voteResult));
        }
        recordsByTime.sort((x, y) -> Long.compare(x.getSendTimestamp(), y.getSendTimestamp()));
        log.info("{} vote records loaded", recordsByTime.size());
    }

    public void run() {
        Thread scheduler = new Thread(this::sendVotesOnTime, "VoteScheduler");
        scheduler.run();
        log.info("VoteScheduler runs");
    }

    private void sendVotesOnTime() {
        while (currentRecordIdx < recordsByTime.size()) {
            for(; currentRecordIdx < recordsByTime.size() && recordsByTime.get(currentRecordIdx).getSendTimestamp() < System.currentTimeMillis() + 1000; currentRecordIdx++) {
                VoteResult voteResult = recordsByTime.get(currentRecordIdx).getVoteResult();
                log.info("Sending vote record {}", voteResult);
                voitingClient.sendVoteResult(voteResult);
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
        log.info("All vote records sent");
    }

}
