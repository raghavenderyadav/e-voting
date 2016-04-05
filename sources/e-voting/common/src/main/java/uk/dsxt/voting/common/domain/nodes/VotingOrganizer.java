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
import uk.dsxt.voting.common.domain.dataModel.*;
import uk.dsxt.voting.common.messaging.MessagesSerializer;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class VotingOrganizer implements NetworkClient {

    private NetworkMessagesSender network;

    private final ScheduledExecutorService calculateResultsService;

    private final PrivateKey privateKey;

    private final CryptoHelper cryptoHelper;

    private final MessagesSerializer messagesSerializer;

    private final Map<String, Participant> participantsById;

    private final PublicKey publicKey;

    private static class VotingRecord {
        Voting voting;
        Map<String, VoteResult> resultsByMessageId = new HashMap<>();
        Map<String, String> serializedResultsByMessageId = new HashMap<>();
        List<VoteStatus> statuses = new ArrayList<>();
    }

    private final Map<String, VotingRecord> votingsById = new HashMap<>();

    public VotingOrganizer(MessagesSerializer messagesSerializer, CryptoHelper cryptoProvider, Map<String, Participant> participantsById, PrivateKey privateKey)
        throws InternalLogicException, GeneralSecurityException {
        calculateResultsService = Executors.newScheduledThreadPool(10);
        this.messagesSerializer = messagesSerializer;
        this.privateKey = privateKey;
        this.cryptoHelper = cryptoProvider;
        this.participantsById = participantsById;
        publicKey = cryptoHelper.loadPublicKey(participantsById.get(MasterNode.MASTER_HOLDER_ID).getPublicKey());
    }

    @Override
    public void setNetworkMessagesSender(NetworkMessagesSender networkMessagesSender) {
        network = networkMessagesSender;
    }

    public void addNewVoting(Voting voting) {
        try {
            network.addVoting(voting);
        } catch (InternalLogicException e) {
            log.error("addNewVoting. addVoting failed. votingId={}", voting.getId(), e);
        }
        calculateResultsService.schedule(() -> calculateResults(voting.getId()), Math.max(voting.getEndTimestamp() - System.currentTimeMillis(), 0) + 60000, TimeUnit.MILLISECONDS);
        log.info("addNewVoting. Voting added. votingId={}", voting.getId());
    }

    private synchronized void calculateResults(String votingId) {
        log.info("calculateResults started. votingId={}", votingId);
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            log.warn("calculateResults. Voting not found {}", votingId);
            return;
        }
        VoteResult totalResult = new VoteResult(votingId, null);
        for (Map.Entry<String, VoteResult> resultEntry : votingRecord.resultsByMessageId.entrySet()) {
            String messageId = resultEntry.getKey();
            VoteResult result = resultEntry.getValue();
            String voteString = votingRecord.serializedResultsByMessageId.get(messageId);
            for (VoteStatus status : votingRecord.statuses) {
                if (!status.getMessageId().equals(messageId))
                    continue;
                if (status.getStatus() != VoteResultStatus.OK) {
                    log.info("calculateResults. Skip result due its VoteStatus {}. messageId={} ownerId={}", status.getStatus(), messageId, result.getHolderId());
                    continue;
                }
                String error = result.findError(votingRecord.voting);
                if (error != null) {
                    log.info("calculateResults. Skip incorrect result. messageId={} ownerId={} error={}", status.getStatus(), messageId, result.getHolderId(), error);
                    continue;
                }
                try {
                    if (!cryptoHelper.verifySignature(result.getHolderId(), status.getVoteSign(), publicKey)) {
                        log.error("calculateResults. VoteStatus with incorrect signature. messageId={} ownerId={}", messageId, result.getHolderId());
                        continue;
                    }
                } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                    log.error("calculateResults. VoteStatus verify signature failed. messageId={} ownerId={} error={}", messageId, result.getHolderId(), e.getMessage());
                    continue;
                }
                try {
                    if (!cryptoHelper.getDigest(voteString).equals(status.getVoteDigest())) {
                        log.error("calculateResults. VoteStatus vote digest is incorrect. messageId={} ownerId={}", messageId, result.getHolderId());
                        continue;
                    }
                } catch (NoSuchAlgorithmException e) {
                    log.error("calculateResults. getDigest failed. messageId={} ownerId={} error={}", messageId, result.getHolderId(), e.getMessage());
                    continue;
                }
                totalResult.add(result);
                break;
            }
        }
        try {
            network.addVotingTotalResult(totalResult, votingRecord.voting);
            log.info("calculateResults. totalResult={}", totalResult);
        } catch (InternalLogicException e) {
            log.info("calculateResults. addVotingTotalResult failed. totalResult={}", totalResult, e);
        }
    }

    @Override
    public synchronized void addVoting(Voting voting) {
        VotingRecord votingRecord = votingsById.get(voting.getId());
        if (votingRecord == null) {
            votingRecord = new VotingRecord();
            votingsById.put(voting.getId(), votingRecord);
        }
        votingRecord.voting = voting;
    }

    @Override
    public synchronized void addVotingTotalResult(VoteResult result) {
    }

    @Override
    public synchronized void addVoteStatus(VoteStatus status) {
        VotingRecord votingRecord = votingsById.get(status.getVotingId());
        if (votingRecord == null) {
            votingRecord = new VotingRecord();
            votingsById.put(status.getVotingId(), votingRecord);
        }
        votingRecord.statuses.add(status);
    }

    @Override
    public void addVote(VoteResult result, String messageId, String serializedResult) {
        VotingRecord votingRecord = votingsById.get(result.getVotingId());
        if (votingRecord == null) {
            votingRecord = new VotingRecord();
            votingsById.put(result.getVotingId(), votingRecord);
        }
        votingRecord.resultsByMessageId.put(messageId, result);
        votingRecord.serializedResultsByMessageId.put(messageId, serializedResult);
    }
}
