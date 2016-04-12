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

import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.joda.time.Instant;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.VoteStatus;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.utils.InternalLogicException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Log4j2
public class VoteStatusSender implements NetworkClient {

    private NetworkMessagesSender network;
    
    private final AtomicLong lastUnsentMessageId = new AtomicLong();
    
    private final ScheduledExecutorService unconfirmedMessagesChecker = Executors.newSingleThreadScheduledExecutor();

    @Value
    private static class StatusRecord {
        public long timestamp;
        public VoteStatus status;
    }
    
    private final Map<String, StatusRecord> statuses = new HashMap<>();

    private final long confirmTimeout;
    
    public VoteStatusSender(long confirmTimeout) {
        this.confirmTimeout = confirmTimeout;
        unconfirmedMessagesChecker.scheduleWithFixedDelay(this::checkUnconfirmedMessages, 1, 1, TimeUnit.MINUTES);        
    }
    
    public void sendVoteStatus(VoteStatus status) {
        String messageId = null;
        try {
            messageId = network.addVoteStatus(status);
        } catch (InternalLogicException e) {
            log.error("sendVoteStatus failed. voteMessageId={} error={}", status.getMessageId(), e.getMessage());
        }
        if (messageId == null) {
            messageId = String.format("UM-%d", lastUnsentMessageId.incrementAndGet());
        }
        StatusRecord record = new StatusRecord(System.currentTimeMillis(), status);
        synchronized (statuses) {
            statuses.put(messageId, record);
        }
    }
    
    private void checkUnconfirmedMessages() {
        List<VoteStatus> unconfirmedStatuses = new ArrayList<>();
        List<String> unconfirmedMessageIds = new ArrayList<>();
        long thresholdTime = System.currentTimeMillis() - confirmTimeout;
        synchronized (statuses) {
            for(Map.Entry<String, StatusRecord> statusEntry : statuses.entrySet()) {
                if (statusEntry.getValue().getTimestamp() < thresholdTime) {
                    log.warn("checkUnconfirmedMessages. Message {} with status of {} was sent at {} - resend.", 
                        statusEntry.getKey(), statusEntry.getValue().getStatus().getMessageId(), new Instant(statusEntry.getValue().getTimestamp()));
                    unconfirmedStatuses.add(statusEntry.getValue().getStatus());
                    unconfirmedMessageIds.add(statusEntry.getKey());
                }
            }
            for(String messageId : unconfirmedMessageIds) {
                statuses.remove(messageId);
            }
        }
        for(VoteStatus status : unconfirmedStatuses) {
            sendVoteStatus(status);
        }
        log.debug("checkUnconfirmedMessages finshed {} messages resent", unconfirmedMessageIds.size());
    }
    
    @Override
    public void setNetworkMessagesSender(NetworkMessagesSender networkMessagesSender) {
        network = networkMessagesSender;
    }

    @Override
    public void addVoteStatus(VoteStatus status, String messageId, boolean isCommitted, boolean isSelf) {
        if (!isCommitted || !isSelf)
            return;
        StatusRecord record;
        synchronized (statuses) {
            record = statuses.remove(messageId);
        }
        if (record == null) {
            log.warn("addVoteStatus. Origin message not found. messageId={}", messageId);
        }
    }

    @Override
    public void addVoting(Voting voting) {
    }

    @Override
    public void addVotingTotalResult(VoteResult result) {
    }

    @Override
    public void addVoteToMaster(VoteResult result, String messageId, String serializedResult, boolean isCommitted, boolean isSelf) {
    }

    @Override
    public void notifyVote(String messageId, boolean isCommitted, boolean isSelf) {
    }
}
