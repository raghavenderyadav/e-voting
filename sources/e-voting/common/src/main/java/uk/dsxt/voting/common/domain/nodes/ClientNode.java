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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.domain.dataModel.Client;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.VoteResultStatus;
import uk.dsxt.voting.common.domain.dataModel.Voting;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class ClientNode implements AssetsHolder, NetworkMessagesReceiver {

    public static final String PATH_SEPARATOR = "/";

    @Setter
    private VoteAcceptor parentHolder;

    @Getter
    private final String participantId;

    private final SortedMap<Long, Map<String, Client>> clientsByIdByTimestamp = new TreeMap<>();

    protected static class VotingRecord {
        public Voting voting;
        public VoteResult totalResult;
        public Map<String, VoteResult> sumClientResultsByClientId = new HashMap<>();
        public Map<String, VoteResult> allClientResultsByClientPath = new HashMap<>();
        public Map<String, VoteResult> confirmedClientResultsByClientPath = new HashMap<>();
    }

    protected final Map<String, VotingRecord> votingsById = new HashMap<>();

    public ClientNode(String participantId) {
        this.participantId = participantId;
    }

    public synchronized void setClientsOnTime(long timestamp, Client[] clients) {
        clientsByIdByTimestamp.put(timestamp, Arrays.stream(clients).collect(Collectors.toMap(Client::getParticipantId, Function.identity())));
    }

    @Override
    public synchronized boolean acceptVote(VoteResult newResult, List<String> signatures) {
        if (!addVote(newResult, false))
            return false;
        if (parentHolder != null)
            parentHolder.acceptVote(new VoteResult(newResult, participantId + PATH_SEPARATOR + newResult.getHolderId()), signatures);
        return true;
    }

    private boolean addVote(VoteResult newResult, boolean isConfirmed) {
        String holdersTreePath = newResult.getHolderId();
        String[] clientIds = holdersTreePath.split(PATH_SEPARATOR);
        if (clientIds.length < 1) {
            log.warn("acceptVote. holdersTreePath is empty. votingId={} holdersTreePath={}", newResult.getVotingId(), holdersTreePath);
            return false;
        }
        VotingRecord votingRecord = votingsById.get(newResult.getVotingId());
        if (votingRecord == null) {
            log.warn("acceptVote. Voting not found {}. holdersTreePath={}", newResult.getVotingId(), holdersTreePath);
            return false;
        }
        //TODO adaptVoteResult

        if (votingRecord.allClientResultsByClientPath.containsKey(holdersTreePath)) {
            log.warn("acceptVote. Duplicate holdersTreePath. votingId={} holdersTreePath={}", newResult.getVotingId(), holdersTreePath);
            return false;
        }
        if (!isConfirmed && votingRecord.voting.getBeginTimestamp() > System.currentTimeMillis()) {
            log.warn("acceptVote. Voting {} does not begin yet. holdersTreePath={}", newResult.getVotingId(), holdersTreePath);
            return false;
        }
        if (!isConfirmed && votingRecord.voting.getEndTimestamp() < System.currentTimeMillis()) {
            log.warn("acceptVote. Voting {} already ends. holdersTreePath={}", newResult.getVotingId(), holdersTreePath);
            return false;
        }

        log.debug("acceptVote. participantId={} votingId={} holdersTreePath={} isConfirmed={}", participantId, newResult.getVotingId(), holdersTreePath, isConfirmed);

        String clientId = clientIds[0];
        if (!isConfirmed && newResult.getStatus() == VoteResultStatus.OK) {
            Client client =  getClient(votingRecord.voting, clientIds[0]);
            if (client == null) {
                log.warn("acceptVote. Client not found on voting begin. votingId={} holdersTreePath={}", newResult.getVotingId(), holdersTreePath);
                return false;
            }

            String resutError = newResult.findError(votingRecord.voting);
            if (resutError != null) {
                log.warn("acceptVote. VoteResult has errors. votingId={} holdersTreePath={} error={}", newResult.getVotingId(), holdersTreePath, resutError);
                newResult.setStatus(VoteResultStatus.ERROR);
            } else {
                VoteResult clientResult = votingRecord.sumClientResultsByClientId.get(clientId);
                if (clientResult == null)
                    clientResult = new VoteResult(newResult.getVotingId(), clientId);
                clientResult = clientResult.sum(newResult);
                if (clientResult.getPacketSize().compareTo(client.getPacketSize()) > 0) {
                    log.warn("acceptVote. VoteResult adds to big packet size to client. votingId={} holdersTreePath={} clientPacketSize={} newPacketSize={}",
                            newResult.getVotingId(), holdersTreePath, client.getPacketSize(), clientResult.getPacketSize());
                    newResult.setStatus(VoteResultStatus.ERROR);
                } else {
                    votingRecord.sumClientResultsByClientId.put(clientId, clientResult);
                }
            }
        }
        if (clientId.equals(participantId) && clientIds.length > 0) {
            holdersTreePath = holdersTreePath.substring(holdersTreePath.indexOf(PATH_SEPARATOR) + 1);
        }
        votingRecord.allClientResultsByClientPath.put(holdersTreePath, newResult);
        if (isConfirmed)
            votingRecord.confirmedClientResultsByClientPath.put(holdersTreePath, newResult);
        return true;
    }

    @Override
    public synchronized Voting getVoting(String votingId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        return votingRecord == null ? null : votingRecord.voting;
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
    public synchronized Collection<VoteResult> getAllClientVotes(String votingId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        return votingRecord == null ? null : votingRecord.allClientResultsByClientPath.values();
    }

    @Override
    public synchronized Collection<VoteResult> getConfirmedClientVotes(String votingId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        return votingRecord == null ? null : votingRecord.confirmedClientResultsByClientPath.values();
    }

    @Override
    public synchronized VoteResult getClientVote(String votingId, String clientId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        return votingRecord == null ? null : votingRecord.allClientResultsByClientPath.get(clientId);
    }

    @Override
    public synchronized void addClientVote(VoteResult result) {
        acceptVote(result, new ArrayList<>());
    }

    @Override
    public synchronized BigDecimal getClientPacketSize(String votingId, String clientId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            log.warn("acceptVote. Voting not found {}. clientId={}", votingId, clientId);
            return null;
        }
        Client client =  getClient(votingRecord.voting, clientId);
        if (client == null) {
            log.warn("acceptVote. Client not found on voting begin. votingId={} clientId={}", votingId, clientId);
            return null;
        }
        return client.getPacketSize();
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

    @Override
    public synchronized void addVote(VoteResult result) {
        addVote(result, true);
    }

    private synchronized Client getClient(Voting voting, String clientId) {
        Map<String, Client> clients = null;
        for (Map.Entry<Long, Map<String, Client>> clientsEntry : clientsByIdByTimestamp.entrySet()) {
            if (clientsEntry.getKey() <= voting.getBeginTimestamp()) {
              clients = clientsEntry.getValue();
            } else {
              break;
            }
        }
        if (clients == null) {
            log.warn("Clients not found on voting begin. votingId={}");
            return null;
        }
        return clients.get(clientId);
    }
}
