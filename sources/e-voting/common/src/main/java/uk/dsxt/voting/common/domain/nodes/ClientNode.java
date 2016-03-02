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
import uk.dsxt.voting.common.domain.dataModel.Client;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.VoteResultStatus;
import uk.dsxt.voting.common.domain.dataModel.Voting;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class ClientNode implements AssetsHolder, BroadcastingMessageConnector {

    private static final String PATH_SEPARATOR = "/";

    private final AssetsHolder parentHolder;

    private final String participantId;

    private final SortedMap<Long, Map<String, Client>> clientsByIdByTimestamp = new TreeMap<>();

    protected static class VotingRecord {
        public Voting voting;
        public VoteResult totalResult;
        public Map<String, VoteResult> sumClientResultsByClientId = new HashMap<>();
        public Map<String, VoteResult> allClientResultsByClientPath = new HashMap<>();
    }

    protected final Map<String, VotingRecord> votingsById = new HashMap<>();

    public ClientNode(AssetsHolder parentHolder, String participantId) {
        this.parentHolder = parentHolder;
        this.participantId = participantId;
    }

    public synchronized void setClientsOnTime(long timestamp, Client[] clients) {
        clientsByIdByTimestamp.put(timestamp, Arrays.stream(clients).collect(Collectors.toMap(Client::getParticipantId, Function.identity())));
    }

    @Override
    public synchronized boolean acceptVote(VoteResult newResult, String clientId, String holdersTreePath) {
        Map<String, Client> clients = null;
        VotingRecord votingRecord = votingsById.get(newResult.getVotingId());
        if (votingRecord == null) {
            log.warn("acceptVote. Voting not found {}", newResult.getVotingId());
            return false;
        }
        if (votingRecord.voting.getBeginTimestamp() > System.currentTimeMillis()) {
            log.warn("acceptVote. Voting {} does not begin yet", newResult.getVotingId());
            return false;
        }
        if (votingRecord.voting.getEndTimestamp() < System.currentTimeMillis()) {
            log.warn("acceptVote. Voting {} already ends", newResult.getVotingId());
            return false;
        }
        for (Map.Entry<Long, Map<String, Client>> clientsEntry : clientsByIdByTimestamp.entrySet()) {
            if (clientsEntry.getKey() <= votingRecord.voting.getBeginTimestamp()) {
                clients = clientsEntry.getValue();
            } else {
                break;
            }
        }
        if (clients == null) {
            log.warn("acceptVote. Clients not found on voting begin. votingId={}", newResult.getVotingId());
            return false;
        }
        Client client = clients.get(clientId);
        if (client == null) {
            log.warn("acceptVote. Client not found on voting begin. votingId={} clientId={}", newResult.getVotingId(), clientId);
            return false;
        }
        if (votingRecord.allClientResultsByClientPath.containsKey(holdersTreePath)) {
            log.warn("acceptVote. Duplicate holdersTreePath. votingId={} clientId={} path={}", newResult.getVotingId(), clientId, holdersTreePath);
            return false;
        }

        if (newResult.getStatus() == VoteResultStatus.OK) {
            String resutError = newResult.findError(votingRecord.voting);
            if (resutError != null) {
                log.warn("acceptVote. VoteResult has errors. votingId={} clientId={} error={}", newResult.getVotingId(), clientId, resutError);
                newResult.setStatus(VoteResultStatus.ERROR);
            } else {
                VoteResult clientResult = votingRecord.sumClientResultsByClientId.get(clientId);
                if (clientResult == null)
                    clientResult = new VoteResult(newResult.getVotingId(), clientId);
                clientResult = clientResult.sum(newResult);
                if (clientResult.getPacketSize().compareTo(client.getPacketSize()) > 0) {
                    log.warn("acceptVote. VoteResult adds to big packet size to client. votingId={} clientId={} clientPacketSize={} newPacketSize={}",
                            newResult.getVotingId(), clientId, client.getPacketSize(), clientResult.getPacketSize());
                    newResult.setStatus(VoteResultStatus.ERROR);
                } else {
                    votingRecord.sumClientResultsByClientId.put(clientId, clientResult);
                }
            }

        }
        votingRecord.allClientResultsByClientPath.put(holdersTreePath, newResult);
        if (parentHolder != null)
            parentHolder.acceptVote(newResult, participantId, participantId + PATH_SEPARATOR + holdersTreePath);
        return true;
    }

    @Override
    public synchronized Collection<Voting> getVotings() {
        return votingsById.values().stream().map(vr -> vr.voting).collect(Collectors.toList());
    }

    @Override
    public synchronized VoteResult getTotalVotingResult(String votingId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        return votingRecord == null ? null : votingRecord.totalResult;
    }

    @Override
    public Map<String, VoteResult> getAllClientVotes(String votingId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        return votingRecord == null ? null : votingRecord.allClientResultsByClientPath;
    }

    @Override
    public synchronized void addVoting(Voting voting) {
        VotingRecord votingRecord = new VotingRecord();
        votingRecord.voting = voting;
        votingsById.put(voting.getId(), votingRecord);
    }

    @Override
    public synchronized void addVotingTotalResult(VoteResult result) {
        VotingRecord votingRecord = votingsById.get(result.getVotingId());
        if (votingRecord == null) {
            log.warn("addVotingTotalResult. Voting not found {}", result.getVotingId());
            return;
        }
        votingRecord.totalResult = result;
    }
}
