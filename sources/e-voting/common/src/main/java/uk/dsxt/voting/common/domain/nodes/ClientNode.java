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

import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.domain.dataModel.*;
import uk.dsxt.voting.common.messaging.MessagesSerializer;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.MessageBuilder;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class ClientNode implements AssetsHolder, NetworkMessagesReceiver {

    protected VoteAcceptor parentHolder;

    @Setter
    protected NetworkMessagesSender network;

    private final String participantId;

    protected final Map<String, Participant> participantsById;

    protected final PrivateKey privateKey;

    protected final CryptoHelper cryptoHelper;

    protected final MessagesSerializer messagesSerializer;

    private final PublicKey masterNodePublicKey;

    private final SortedMap<Long, Map<String, Client>> clientsByIdByTimestamp = new TreeMap<>();

    private static class VotingRecord {
        Voting voting;
        VoteResult totalResult;
        Map<String, Client> clients = new HashMap<>();
        BigDecimal totalResidual;
        Map<String, VoteResult> clientResultsByClientId = new HashMap<>();
        Map<String, VoteResult> clientResultsByMessageId = new HashMap<>();
        Map<String, BigDecimal> clientResidualsByClientId = new HashMap<>();
        List<VoteStatus> voteStatuses = new ArrayList<>();
    }

    private final Map<String, VotingRecord> votingsById = new HashMap<>();

    public ClientNode(String participantId, MessagesSerializer messagesSerializer, CryptoHelper cryptoProvider, Map<String, Participant> participantsById, PrivateKey privateKey, VoteAcceptor parentHolder)
            throws InternalLogicException, GeneralSecurityException {
        this.participantId = participantId;
        this.messagesSerializer = messagesSerializer;
        this.privateKey = privateKey;
        this.cryptoHelper = cryptoProvider;
        this.participantsById = participantsById;
        this.parentHolder = parentHolder;
        Participant masterNode = participantsById.get(MasterNode.MASTER_HOLDER_ID);
        if (masterNode == null)
            throw new InternalLogicException(String.format("Master node %s not found", MasterNode.MASTER_HOLDER_ID));
        if (masterNode.getPublicKey() == null)
            throw new InternalLogicException(String.format("Master node %s has no public key", MasterNode.MASTER_HOLDER_ID));
        masterNodePublicKey = cryptoProvider.loadPublicKey(masterNode.getPublicKey());
    }

    public synchronized void setClientsOnTime(long timestamp, Client[] clients) {
        clientsByIdByTimestamp.put(timestamp, Arrays.stream(clients).collect(Collectors.toMap(Client::getParticipantId, Function.identity())));
        long now = System.currentTimeMillis();
        votingsById.values().stream().filter(vr -> vr.voting.getBeginTimestamp() > now).forEach(this::setVotingClients);
    }

    @Override
    public synchronized VoteResultStatus acceptVote(String transactionId, String votingId, BigDecimal packetSize, String clientId, BigDecimal clientPacketResidual, String encryptedData, String clientSignature) {
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            return VoteResultStatus.IncorrectMessage;
        }
        Client client = votingRecord.clients.get(clientId);
        if (client == null) {
            return VoteResultStatus.IncorrectMessage;
        }
        Participant participant = participantsById.get(clientId);
        if (participant == null) {
            return VoteResultStatus.IncorrectMessage;
        }
        if (participant.getPublicKey() == null) {
            return VoteResultStatus.IncorrectMessage;
        }
        String msg = buildMessage(transactionId, votingId, packetSize, clientId, clientPacketResidual, encryptedData);
        try {
            if (!cryptoHelper.verifySignature(msg, clientSignature, cryptoHelper.loadPublicKey(participant.getPublicKey()))) {
                return VoteResultStatus.SignatureFailed;
            }
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            return VoteResultStatus.SignatureFailed;
        }
        return addVoteAndHandleErrors(votingRecord, client, transactionId, packetSize, clientPacketResidual, encryptedData);
    }

    private synchronized VoteResultStatus addVoteAndHandleErrors(VotingRecord votingRecord, Client client, String transactionId, BigDecimal packetSize, BigDecimal clientPacketResidual, String encryptedData) {
        VoteResultStatus status = addVote(votingRecord, client, transactionId, packetSize, clientPacketResidual, encryptedData);
        if (status != VoteResultStatus.OK) {
            network.addVoteStatus(new VoteStatus(votingRecord.voting.getId(), transactionId, status, AssetsHolder.EMPTY_SIGNATURE));
        }
        return status;
    }

    private synchronized VoteResultStatus addVote(VotingRecord votingRecord, Client client, String transactionId, BigDecimal packetSize, BigDecimal clientPacketResidual, String encryptedData) {
        log.debug("acceptVote. participantId={} votingId={} client={}", participantId, votingRecord.voting.getId(), client.getParticipantId());

        if (votingRecord.voting.getBeginTimestamp() > System.currentTimeMillis()) {
            log.warn("acceptVote. Voting {} does not begin yet. client={}", votingRecord.voting.getId(), client.getParticipantId());
            return VoteResultStatus.IncorrectMessage;
        }
        if (votingRecord.voting.getEndTimestamp() < System.currentTimeMillis()) {
            log.warn("acceptVote. Voting {} already ends. client={}", votingRecord.voting.getId(), client.getParticipantId());
            return VoteResultStatus.IncorrectMessage;
        }
        if (packetSize == null || packetSize.signum() != 1) {
            log.warn("acceptVote. invalid packetSize {}. Voting={} client={}", packetSize, votingRecord.voting.getId(), client.getParticipantId());
            return VoteResultStatus.IncorrectMessage;
        }
        if (clientPacketResidual == null || clientPacketResidual.signum() < 0) {
            log.warn("acceptVote. invalid clientPacketResidual {}. Voting={} client={}", clientPacketResidual, votingRecord.voting.getId(), client.getParticipantId());
            return VoteResultStatus.IncorrectMessage;
        }
        BigDecimal prevResidual = votingRecord.clientResidualsByClientId.get(client.getParticipantId());
        if (prevResidual == null) {
            prevResidual = client.getPacketSizeBySecurity().get(votingRecord.voting.getSecurity());
        }
        if (prevResidual.subtract(packetSize).compareTo(clientPacketResidual) != 0) {
            log.warn("acceptVote. invalid clientPacketResidual {} (expected {}). Voting={} client={}", clientPacketResidual, prevResidual.subtract(packetSize), votingRecord.voting.getId(), client.getParticipantId());
            return VoteResultStatus.IncorrectResidual;
        }

        votingRecord.clientResidualsByClientId.put(client.getParticipantId(), clientPacketResidual);
        votingRecord.totalResidual = votingRecord.totalResidual.subtract(packetSize);
        
        
        if (parentHolder != null) {
            String encrypted = null, sign = null;
            try {
                encrypted = encryptToMasterNode(encryptedData, participantId, cryptoHelper.createSignature(MessageBuilder.buildMessage(encryptedData, participantId), privateKey));
                String signed = buildMessage(transactionId, votingRecord.voting.getId(), packetSize, participantId, votingRecord.totalResidual, encrypted);
                sign = cryptoHelper.createSignature(signed, privateKey);
            } catch (GeneralSecurityException | UnsupportedEncodingException | InternalLogicException e) {
                log.error("acceptVote. encrypt or sign message to parent failed. voting={} client={} error={}", votingRecord.voting.getId(), client.getParticipantId(), e.getMessage());
            }
            if (sign != null) {
                parentHolder.acceptVote(transactionId, votingRecord.voting.getId(), packetSize, participantId, votingRecord.totalResidual, encrypted, sign);
            }
        }
        return VoteResultStatus.OK;
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
        return votingRecord == null ? null : votingRecord.clientResultsByClientId.values();
    }

    @Override
    public synchronized Collection<VoteStatus> getConfirmedClientVotes(String votingId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        return votingRecord == null ? null : votingRecord.voteStatuses;
    }

    @Override
    public synchronized VoteResult getClientVote(String votingId, String clientId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        return votingRecord == null ? null : votingRecord.clientResultsByClientId.get(clientId);
    }

    @Override
    public synchronized String addClientVote(VoteResult result, String signature) throws InternalLogicException {
        VotingRecord votingRecord = votingsById.get(result.getVotingId());
        if (votingRecord == null) {
            throw new InternalLogicException(String.format("acceptVote. Voting not found. votingId=%s clientId=%s", result.getVotingId(), result.getHolderId()));
        }
        Client client = votingRecord.clients.get(result.getHolderId());
        if (client == null) {
            throw new InternalLogicException(String.format("acceptVote. Client not found on voting begin. votingId=%s clientId=%s", result.getVotingId(), result.getHolderId()));
        }
        String nodeSignature;
        try {
            nodeSignature = cryptoHelper.createSignature(messagesSerializer.serialize(result), privateKey);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            throw new InternalLogicException("Can not sign vote");
        }
        String tranId = network.addVote(result, signature, nodeSignature);
        addVoteAndHandleErrors(votingRecord, client, tranId, result.getPacketSize(), client.getPacketSizeBySecurity().get(votingRecord.voting.getSecurity()).subtract(result.getPacketSize()), 
            encryptToMasterNode(messagesSerializer.serialize(result), signature));
        votingRecord.clientResultsByClientId.put(result.getHolderId(), result);
        votingRecord.clientResultsByMessageId.put(tranId, result);
        return tranId;
    }

    @Override
    public synchronized BigDecimal getClientPacketSize(String votingId, String clientId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            log.warn("acceptVote. Voting not found {}. clientId={}", votingId, clientId);
            return null;
        }
        Client client = votingRecord.clients.get(clientId);
        if (client == null) {
            log.warn("acceptVote. Client not found on voting begin. votingId={} clientId={}", votingId, clientId);
            return null;
        }
        return client.getPacketSizeBySecurity().get(votingRecord.voting.getSecurity());
    }

    @Override
    public synchronized void addVoting(Voting voting) {
        VotingRecord votingRecord = new VotingRecord();
        votingRecord.voting = voting;
        setVotingClients(votingRecord);
        votingsById.put(voting.getId(), votingRecord);
    }
    
    private void setVotingClients(VotingRecord votingRecord) {
        String security = votingRecord.voting.getSecurity(); 
        for (Map.Entry<Long, Map<String, Client>> clientsEntry : clientsByIdByTimestamp.entrySet()) {
            if (clientsEntry.getKey() <= votingRecord.voting.getBeginTimestamp()) {
                votingRecord.clients = new HashMap<>();
                votingRecord.totalResidual = BigDecimal.ZERO;
                for(Client client : clientsEntry.getValue().values()) {
                    if (client.getPacketSizeBySecurity() == null)
                        continue;
                    BigDecimal packetSize = client.getPacketSizeBySecurity().get(security);
                    if (packetSize == null || packetSize.signum() <= 0)
                        continue;
                    votingRecord.clients.put(client.getParticipantId(), client);
                    votingRecord.totalResidual = votingRecord.totalResidual.add(packetSize);
                }
            } else {
                break;
            }
        }
    }

    @Override
    public synchronized void addVotingTotalResult(VoteResult result) {
        VotingRecord votingRecord = votingsById.get(result.getVotingId());
        if (votingRecord == null) {
            log.warn("addVotingTotalResult. Voting not found {}", result.getVotingId());
            return;
        }
        //TODO: move to common logic
        try {
            votingRecord.totalResult = messagesSerializer.adaptVoteResultFromXML(result, votingRecord.voting);
        } catch (Exception e) {
            log.error("addVotingTotalResult failed", e);
        }
    }

    @Override
    public synchronized void addVoteStatus(VoteStatus status) {
        VotingRecord votingRecord = votingsById.get(status.getVotingId());
        if (votingRecord == null) {
            log.warn("addVoteStatus. Voting not found {}", status.getVotingId());
            return;
        }
        votingRecord.voteStatuses.add(status);
    }

    @Override
    public synchronized void addVote(VoteResult result, String messageId) {
    }

    private String buildMessage(String transactionId, String votingId, BigDecimal packetSize, String clientId, BigDecimal clientPacketResidual, String encryptedData) {
        return MessageBuilder.buildMessage(transactionId, votingId, packetSize.toPlainString(), clientId, clientPacketResidual.toPlainString(), encryptedData);
    }

    private String encryptToMasterNode(String... data) throws InternalLogicException {
        String text = MessageBuilder.buildMessage(data);
        try {
            return cryptoHelper.encrypt(text, masterNodePublicKey);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            throw new InternalLogicException(String.format("Can not encrypt message: %s", e.getMessage()));
        }
    }
}
