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
import uk.dsxt.voting.common.utils.CollectionsHelper;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.MessageBuilder;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.function.Consumer;
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

    private final Map<String, VotingRecord> votingsById = new HashMap<>();

    private final Consumer<String> stateSaver;

    public ClientNode(String participantId, MessagesSerializer messagesSerializer, CryptoHelper cryptoProvider, Map<String, Participant> participantsById, PrivateKey privateKey,
                      VoteAcceptor parentHolder, String state, Consumer<String> stateSaver)
        throws InternalLogicException, GeneralSecurityException {
        this.participantId = participantId;
        this.messagesSerializer = messagesSerializer;
        this.privateKey = privateKey;
        this.cryptoHelper = cryptoProvider;
        this.participantsById = participantsById;
        this.parentHolder = parentHolder;
        this.stateSaver = stateSaver;
        Participant masterNode = participantsById.get(MasterNode.MASTER_HOLDER_ID);
        if (masterNode == null)
            throw new InternalLogicException(String.format("Master node %s not found", MasterNode.MASTER_HOLDER_ID));
        if (masterNode.getPublicKey() == null)
            throw new InternalLogicException(String.format("Master node %s has no public key", MasterNode.MASTER_HOLDER_ID));
        masterNodePublicKey = cryptoProvider.loadPublicKey(masterNode.getPublicKey());
        if (state != null)
            loadState(state);
    }

    @Override
    public void setNetworkMessagesSender(NetworkMessagesSender networkMessagesSender) {
        network = networkMessagesSender;
    }

    public synchronized void setClientsOnTime(long timestamp, Client[] clients) {
        clientsByIdByTimestamp.put(timestamp, Arrays.stream(clients).collect(Collectors.toMap(Client::getParticipantId, Function.identity())));
        long now = System.currentTimeMillis();
        votingsById.values().stream().filter(vr -> vr.voting != null && vr.voting.getBeginTimestamp() > now).forEach(this::setVotingClients);
    }

    @Override
    public synchronized NodeVoteReceipt acceptVote(String transactionId, String votingId, BigDecimal packetSize, String clientId, BigDecimal clientPacketResidual, 
                                                   String encryptedData, String voteDigest, String clientSignature) throws InternalLogicException {
        String inputMessage = buildMessage(transactionId, votingId, packetSize, clientId, clientPacketResidual, encryptedData, voteDigest);
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
                            status = addVoteAndHandleErrors(votingRecord, client, transactionId, packetSize, clientPacketResidual, encryptedData, voteDigest);
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

    private synchronized VoteResultStatus addVoteAndHandleErrors(VotingRecord votingRecord, Client client, String transactionId, BigDecimal packetSize, BigDecimal clientPacketResidual, String encryptedData, String voteDigest) throws InternalLogicException {
        VoteResultStatus status = checkVote(votingRecord, client, packetSize, clientPacketResidual);
        if (status != VoteResultStatus.OK) {
            network.addVoteStatus(new VoteStatus(votingRecord.voting.getId(), transactionId, status, voteDigest, AssetsHolder.EMPTY_SIGNATURE));
        } else {
            addVote(votingRecord, client, transactionId, packetSize, clientPacketResidual, encryptedData, voteDigest);
        }
        return status;
    }

    private synchronized VoteResultStatus checkVote(VotingRecord votingRecord, Client client, BigDecimal packetSize, BigDecimal clientPacketResidual) {
        if (votingRecord.voting.getBeginTimestamp() > System.currentTimeMillis()) {
            log.warn("checkVote. Voting {} does not begin yet. client={}", votingRecord.voting.getId(), client.getParticipantId());
            return VoteResultStatus.IncorrectMessage;
        }
        if (votingRecord.voting.getEndTimestamp() < System.currentTimeMillis()) {
            log.warn("checkVote. Voting {} already ends. client={}", votingRecord.voting.getId(), client.getParticipantId());
            return VoteResultStatus.IncorrectMessage;
        }
        if (packetSize == null || packetSize.signum() != 1) {
            log.warn("checkVote. invalid packetSize {}. Voting={} client={}", packetSize, votingRecord.voting.getId(), client.getParticipantId());
            return VoteResultStatus.IncorrectMessage;
        }
        if (clientPacketResidual == null || clientPacketResidual.signum() < 0) {
            log.warn("checkVote. invalid clientPacketResidual {}. Voting={} client={}", clientPacketResidual, votingRecord.voting.getId(), client.getParticipantId());
            return VoteResultStatus.IncorrectMessage;
        }
        BigDecimal prevResidual = votingRecord.clientResidualsByClientId.get(client.getParticipantId());
        if (prevResidual == null) {
            prevResidual = client.getPacketSizeBySecurity().get(votingRecord.voting.getSecurity());
        }
        if (prevResidual.subtract(packetSize).compareTo(clientPacketResidual) != 0) {
            log.warn("checkVote. invalid clientPacketResidual {} (expected {}). Voting={} client={}", clientPacketResidual, prevResidual.subtract(packetSize), votingRecord.voting.getId(), client.getParticipantId());
            return VoteResultStatus.IncorrectResidual;
        }
        
        return VoteResultStatus.OK;
    }

    private synchronized void addVote(VotingRecord votingRecord, Client client, String transactionId, BigDecimal packetSize, BigDecimal clientPacketResidual, String encryptedData, String voteDigest) throws InternalLogicException {
        log.debug("addVote. participantId={} votingId={} client={}", participantId, votingRecord.voting.getId(), client.getParticipantId());

        votingRecord.clientResidualsByClientId.put(client.getParticipantId(), clientPacketResidual);
        votingRecord.totalResidual = votingRecord.totalResidual.subtract(packetSize);
        
        if (parentHolder != null) {
            String encrypted = null, sign = null;
            try {
                encrypted = encryptToMasterNode(encryptedData, participantId, cryptoHelper.createSignature(MessageBuilder.buildMessage(encryptedData, participantId), privateKey));
                String signed = buildMessage(transactionId, votingRecord.voting.getId(), packetSize, participantId, votingRecord.totalResidual, encrypted, voteDigest);
                sign = cryptoHelper.createSignature(signed, privateKey);
            } catch (GeneralSecurityException | UnsupportedEncodingException | InternalLogicException e) {
                log.error("addVote. encrypt or sign message to parent failed. voting={} client={} error={}", votingRecord.voting.getId(), client.getParticipantId(), e.getMessage());
            }
            if (sign != null) {
                parentHolder.acceptVote(transactionId, votingRecord.voting.getId(), packetSize, participantId, votingRecord.totalResidual, encrypted, voteDigest, sign);
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
    public synchronized Collection<VoteStatus> getVoteStatuses(String votingId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        return votingRecord == null ? null : votingRecord.voteStatusesByMessageId.values();
    }

    @Override
    public Collection<VoteResultAndStatus> getClientVotes(String votingId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            return null;
        }
        List<VoteResultAndStatus> resultSet = new ArrayList<>();
        for(VoteResult clientResult : votingRecord.clientResultsByClientId.values()) {
            resultSet.add(getClientVote(votingRecord, clientResult));
        }
        return resultSet;
    }

    @Override
    public synchronized VoteResultAndStatus getClientVote(String votingId, String clientId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            return null;
        }
        VoteResult clientResult = votingRecord.clientResultsByClientId.get(clientId);
        if (clientResult == null) {
            return null;
        }
        return getClientVote(votingRecord, clientResult);
    }
    
    private VoteResultAndStatus getClientVote(VotingRecord votingRecord, VoteResult clientResult) {
        String messageId = votingRecord.clientResultMessageIdsByClientId.get(clientResult.getHolderId());
        return new VoteResultAndStatus(clientResult, messageId == null ? null : votingRecord.voteStatusesByMessageId.get(messageId));
    }

    @Override
    public synchronized ClientVoteReceipt addClientVote(VoteResult result, String signature) throws InternalLogicException {
        log.debug("addClientVote votingId={} ownerId={}", result.getVotingId(), result.getHolderId());
        VotingRecord votingRecord = votingsById.get(result.getVotingId());
        if (votingRecord == null) {
            throw new InternalLogicException(String.format("addClientVote. Voting not found. votingId=%s clientId=%s", result.getVotingId(), result.getHolderId()));
        }
        Client client = votingRecord.clients.get(result.getHolderId());
        if (client == null) {
            throw new InternalLogicException(String.format("addClientVote. Client not found on voting begin. votingId=%s clientId=%s", result.getVotingId(), result.getHolderId()));
        }
        String error = result.findError(votingRecord.voting);
        if (error != null) {
            throw new InternalLogicException(String.format("addClientVote. Incorrect vote. votingId=%s clientId=%s. error=%s", result.getVotingId(), result.getHolderId(), error));
        }
        String nodeSignature;
        String serializedVote = messagesSerializer.serialize(result, votingRecord.voting);
        try {
            nodeSignature = cryptoHelper.createSignature(serializedVote, privateKey);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            throw new InternalLogicException("Can not sign vote");
        }
        String tranId = network.addVote(result, votingRecord.voting, signature, nodeSignature);
        String voteDigest;
        try {
            voteDigest = cryptoHelper.getDigest(serializedVote);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalLogicException("Can not calculate hash");
        }
        addVoteAndHandleErrors(votingRecord, client, tranId, result.getPacketSize(), client.getPacketSizeBySecurity().get(votingRecord.voting.getSecurity()).subtract(result.getPacketSize()),
            encryptToMasterNode(serializedVote, signature), voteDigest);
        votingRecord.clientResultsByClientId.put(result.getHolderId(), result);
        votingRecord.clientResultMessageIdsByClientId.put(result.getHolderId(), tranId);
        
        long now = System.currentTimeMillis();
        String resultMessage = messagesSerializer.serialize(result, votingRecord.voting);
        String signedText = MessageBuilder.buildMessage(resultMessage, tranId, voteDigest, Long.toString(now));
        String receiptSign;
        try {
            receiptSign = cryptoHelper.createSignature(signedText, privateKey);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            throw new InternalLogicException("Can not sign vote");
        }
        if (stateSaver != null)
            stateSaver.accept(collectState());
        return new ClientVoteReceipt(resultMessage, tranId, voteDigest, now, receiptSign);
    }

    @Override
    public synchronized BigDecimal getClientPacketSize(String votingId, String clientId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            log.warn("getClientPacketSize. Voting not found {}. clientId={}", votingId, clientId);
            return null;
        }
        Client client = votingRecord.clients.get(clientId);
        if (client == null) {
            log.warn("getClientPacketSize. Client not found on voting begin. votingId={} clientId={}", votingId, clientId);
            return null;
        }
        return client.getPacketSizeBySecurity().get(votingRecord.voting.getSecurity());
    }

    @Override
    public synchronized void addVoting(Voting voting) {
        VotingRecord votingRecord = CollectionsHelper.getOrAdd(votingsById, voting.getId(), VotingRecord::new);
        votingRecord.voting = voting;
        setVotingClients(votingRecord);
    }

    private void setVotingClients(VotingRecord votingRecord) {
        String security = votingRecord.voting.getSecurity();
        for (Map.Entry<Long, Map<String, Client>> clientsEntry : clientsByIdByTimestamp.entrySet()) {
            if (clientsEntry.getKey() <= votingRecord.voting.getBeginTimestamp()) {
                votingRecord.clients = new HashMap<>();
                BigDecimal totalResidual = BigDecimal.ZERO;
                for (Client client : clientsEntry.getValue().values()) {
                    if (client.getPacketSizeBySecurity() == null)
                        continue;
                    BigDecimal packetSize = client.getPacketSizeBySecurity().get(security);
                    if (packetSize == null || packetSize.signum() <= 0)
                        continue;
                    votingRecord.clients.put(client.getParticipantId(), client);
                    totalResidual = totalResidual.add(packetSize);
                }
                if (votingRecord.clientResidualsByClientId.size() == 0)
                    votingRecord.totalResidual = totalResidual;
            } else {
                break;
            }
        }
        log.debug("setVotingClients. votingId={} participantId={} totalResidual={}", votingRecord.voting.getId(), participantId, votingRecord.totalResidual);
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

    protected String buildMessage(String transactionId, String votingId, BigDecimal packetSize, String clientId, BigDecimal clientPacketResidual, String encryptedData, String voteDigest) {
        return MessageBuilder.buildMessage(transactionId, votingId, packetSize.toPlainString(), clientId, clientPacketResidual.toPlainString(), encryptedData, voteDigest);
    }

    private String encryptToMasterNode(String... data) throws InternalLogicException {
        String text = MessageBuilder.buildMessage(data);
        try {
            return cryptoHelper.encrypt(text, masterNodePublicKey);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            throw new InternalLogicException(String.format("Can not encrypt message: %s", e.getMessage()));
        }
    }


    private synchronized void loadState(String state) {
        String[] terms = MessageBuilder.splitMessage(state);
        votingsById.clear();
        for(int i = 0; i < terms.length-1; i+=2) {
            votingsById.put(terms[i], new VotingRecord(terms[i+1]));
        }
    }

    private synchronized String collectState() {
        List<String> stateParts = new ArrayList<>();
        for(Map.Entry<String, VotingRecord> recordEntry : votingsById.entrySet()) {
            stateParts.add(recordEntry.getKey());
            stateParts.add(recordEntry.getValue().toString());
        }
        return MessageBuilder.buildMessage(stateParts.toArray(new String[stateParts.size()]));
    }
}
