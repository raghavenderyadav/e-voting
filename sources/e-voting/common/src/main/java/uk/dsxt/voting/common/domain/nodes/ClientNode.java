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
public class ClientNode implements AssetsHolder, NetworkClient {

    protected VoteAcceptor parentHolder;

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
        Map<String, String> clientResultMessageIdsByClientId = new HashMap<>();
        Map<String, BigDecimal> clientResidualsByClientId = new HashMap<>();
        Map<String, VoteStatus> voteStatusesByMessageId = new HashMap<>();
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

    @Override
    public void setNetworkMessagesSender(NetworkMessagesSender networkMessagesSender) {
        network = networkMessagesSender;
    }

    public synchronized void setClientsOnTime(long timestamp, Client[] clients) {
        clientsByIdByTimestamp.put(timestamp, Arrays.stream(clients).collect(Collectors.toMap(Client::getParticipantId, Function.identity())));
        long now = System.currentTimeMillis();
        votingsById.values().stream().filter(vr -> vr.voting.getBeginTimestamp() > now).forEach(this::setVotingClients);
    }

    @Override
    public synchronized NodeVoteReceipt acceptVote(String transactionId, String votingId, BigDecimal packetSize, String clientId, BigDecimal clientPacketResidual, String encryptedData, String clientSignature) throws InternalLogicException {
        String inputMessage = buildMessage(transactionId, votingId, packetSize, clientId, clientPacketResidual, encryptedData);
        VoteResultStatus status;
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            status = VoteResultStatus.IncorrectMessage;
        } else {
            Client client = votingRecord.clients.get(clientId);
            if (client == null) {
                status = VoteResultStatus.IncorrectMessage;
            } else {
                Participant participant = participantsById.get(clientId);
                if (participant == null) {
                    status = VoteResultStatus.IncorrectMessage;
                } else if (participant.getPublicKey() == null) {
                    status = VoteResultStatus.IncorrectMessage;
                } else {
                    try {
                        if (!cryptoHelper.verifySignature(inputMessage, clientSignature, cryptoHelper.loadPublicKey(participant.getPublicKey()))) {
                            status = VoteResultStatus.SignatureFailed;
                        } else {
                            status = addVoteAndHandleErrors(votingRecord, client, transactionId, packetSize, clientPacketResidual, encryptedData);
                        }
                    } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                        status = VoteResultStatus.SignatureFailed;
                    }
                }
            }
        }
        long now = System.currentTimeMillis();
        String signedText = MessageBuilder.buildMessage(inputMessage, Long.toString(now), status.toString());
        String receiptSign;
        try {
            receiptSign = cryptoHelper.createSignature(signedText, privateKey);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            throw new InternalLogicException("Can not sign vote");
        }
        return new NodeVoteReceipt(inputMessage, now, status, receiptSign);
    }

    private synchronized VoteResultStatus addVoteAndHandleErrors(VotingRecord votingRecord, Client client, String transactionId, BigDecimal packetSize, BigDecimal clientPacketResidual, String encryptedData) throws InternalLogicException {
        VoteResultStatus status = checkVote(votingRecord, client, packetSize, clientPacketResidual, encryptedData);
        if (status != VoteResultStatus.OK) {
            network.addVoteStatus(new VoteStatus(votingRecord.voting.getId(), transactionId, status, AssetsHolder.EMPTY_SIGNATURE));
        } else {
            addVote(votingRecord, client, transactionId, packetSize, clientPacketResidual, encryptedData);
        }
        return status;
    }

    private synchronized VoteResultStatus checkVote(VotingRecord votingRecord, Client client, BigDecimal packetSize, BigDecimal clientPacketResidual, String encryptedData) {
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
        return VoteResultStatus.OK;
    }

    private synchronized void addVote(VotingRecord votingRecord, Client client, String transactionId, BigDecimal packetSize, BigDecimal clientPacketResidual, String encryptedData) throws InternalLogicException {
        log.debug("acceptVote. participantId={} votingId={} client={}", participantId, votingRecord.voting.getId(), client.getParticipantId());

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
    public synchronized VoteStatus getClientVoteStatus(String votingId, String clientId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            return null;
        }
        String messageId = votingRecord.clientResultMessageIdsByClientId.get(clientId);
        if (messageId == null) {
            return null;
        }
        return votingRecord.voteStatusesByMessageId.get(messageId);
    }

    @Override
    public synchronized Collection<VoteStatus> getVoteStatuses(String votingId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        return votingRecord == null ? null : votingRecord.voteStatusesByMessageId.values();
    }

    @Override
    public synchronized VoteResult getClientVote(String votingId, String clientId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        return votingRecord == null ? null : votingRecord.clientResultsByClientId.get(clientId);
    }

    @Override
    public synchronized ClientVoteReceipt addClientVote(VoteResult result, String signature) throws InternalLogicException {
        VotingRecord votingRecord = votingsById.get(result.getVotingId());
        if (votingRecord == null) {
            throw new InternalLogicException(String.format("acceptVote. Voting not found. votingId=%s clientId=%s", result.getVotingId(), result.getHolderId()));
        }
        Client client = votingRecord.clients.get(result.getHolderId());
        if (client == null) {
            throw new InternalLogicException(String.format("acceptVote. Client not found on voting begin. votingId=%s clientId=%s", result.getVotingId(), result.getHolderId()));
        }
        String nodeSignature;
        String serializedVote = messagesSerializer.serialize(result, votingRecord.voting);
        try {
            nodeSignature = cryptoHelper.createSignature(serializedVote, privateKey);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            throw new InternalLogicException("Can not sign vote");
        }
        String tranId = network.addVote(result, votingRecord.voting, signature, nodeSignature);
        addVoteAndHandleErrors(votingRecord, client, tranId, result.getPacketSize(), client.getPacketSizeBySecurity().get(votingRecord.voting.getSecurity()).subtract(result.getPacketSize()),
            encryptToMasterNode(serializedVote, signature));
        votingRecord.clientResultsByClientId.put(result.getHolderId(), result);
        votingRecord.clientResultMessageIdsByClientId.put(result.getHolderId(), tranId);
        
        long now = System.currentTimeMillis();
        String resultMessage = messagesSerializer.serialize(result, votingRecord.voting);
        String signedText = MessageBuilder.buildMessage(resultMessage, tranId, Long.toString(now));
        String receiptSign;
        try {
            receiptSign = cryptoHelper.createSignature(signedText, privateKey);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            throw new InternalLogicException("Can not sign vote");
        }
        return new ClientVoteReceipt(resultMessage, tranId, now, receiptSign);
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
                for (Client client : clientsEntry.getValue().values()) {
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
        votingRecord.totalResult = result;
    }

    @Override
    public synchronized void addVoteStatus(VoteStatus status) {
        VotingRecord votingRecord = votingsById.get(status.getVotingId());
        if (votingRecord == null) {
            log.warn("addVoteStatus. Voting not found {}", status.getVotingId());
            return;
        }
        votingRecord.voteStatusesByMessageId.put(status.getMessageId(), status);
    }

    @Override
    public synchronized void addVote(VoteResult result, String messageId) {
    }

    protected String buildMessage(String transactionId, String votingId, BigDecimal packetSize, String clientId, BigDecimal clientPacketResidual, String encryptedData) {
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
