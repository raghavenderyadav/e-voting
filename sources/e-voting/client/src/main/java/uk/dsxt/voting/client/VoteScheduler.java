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
import org.joda.time.Instant;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.nodes.AssetsHolder;
import uk.dsxt.voting.common.utils.InternalLogicException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class VoteScheduler {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    public VoteScheduler(AssetsHolder assetsHolder, String messagesFileContent, String holderId) throws InternalLogicException {

        if (messagesFileContent == null) {
            log.info("messagesFile not found");
            return;
        }

        String[] lines = messagesFileContent.split("\\r?\\n");
        int cnt = 0, maxDelay = 0;
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
                int delay = Integer.parseInt(terms[0]);
                if (delay <= 0) {
                    log.debug("Immediate vote votingId={} ownerId={}", voteResult.getVotingId(), voteResult.getHolderId());
                    assetsHolder.addClientVote(voteResult, AssetsHolder.EMPTY_SIGNATURE);
                } else {
                    log.debug("Schedule vote votingId={} ownerId={} on {}", voteResult.getVotingId(), voteResult.getHolderId(), new Instant(System.currentTimeMillis() + delay*1000));
                    scheduler.schedule(() -> assetsHolder.addClientVote(voteResult, AssetsHolder.EMPTY_SIGNATURE), delay, TimeUnit.SECONDS);
                }

                cnt++;
                if (maxDelay < delay)
                    maxDelay = delay;
            }
        }
        log.info("VoteScheduler #{} loaded {} records, maxDelay={}", holderId, cnt, maxDelay);
    }

    public void stop() {
        scheduler.shutdown();
    }
}
