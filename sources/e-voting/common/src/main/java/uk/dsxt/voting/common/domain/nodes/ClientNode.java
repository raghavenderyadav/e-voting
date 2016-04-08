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
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<String, VotingRecord> votingsById = new ConcurrentHashMap<>();

    private final Consumer<String> stateSaver;
    
    private final static long VOTE_MAX_DELAY = 60000;

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

    public void setClientsOnTime(long timestamp, Client[] clients) {
        clientsByIdByTimestamp.put(timestamp, Arrays.stream(clients).collect(Collectors.toMap(Client::getParticipantId, Function.identity())));
        long now = System.currentTimeMillis();
        votingsById.values().stream().filter(vr -> vr.voting != null && vr.voting.getBeginTimestamp() > now).forEach(this::setVotingClients);
    }

    @Override
    public NodeVoteReceipt acceptVote(String transactionId, String votingId, BigDecimal packetSize, String clientId, BigDecimal clientPacketResidual, 
                                                   String encryptedData, String voteDigest, String clientSignature) throws InternalLogicException {
        log.debug("acceptVote votingId={} clientId={} packetSize={}", votingId, clientId, packetSize);
        String inputMessage = buildMessage(transactionId, votingId, packetSize, clientId, clientPacketResidual, encryptedData, voteDigest);
        VoteResultStatus status;
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            log.error("acceptVote. voting not found. clientId={} transactionId={}", clientId, transactionId);
            status = VoteResultStatus.IncorrectMessage;
        } else {
            Client client;
            synchronized (votingRecord) {
                client = votingRecord.clients.get(clientId);
            }
            if (client == null) {
                log.error("acceptVote. client not found. clientId={} transactionId={}", clientId, transactionId);
                status = VoteResultStatus.IncorrectMessage;
            } else {
                Participant participant = participantsById.get(clientId);
                if (participant == null) {
                    log.error("acceptVote. participant not found. clientId={} transactionId={}", clientId, transactionId);
                    status = VoteResultStatus.IncorrectMessage;
                } else if (participant.getPublicKey() == null) {
                    log.error("acceptVote. participant has no public key. clientId={} transactionId={}", clientId, transactionId);
                    status = VoteResultStatus.IncorrectMessage;
                } else {
                    try {
                        if (!cryptoHelper.verifySignature(inputMessage, clientSignature, cryptoHelper.loadPublicKey(participant.getPublicKey()))) {
                            log.error("acceptVote. Signature is incorrect. clientId={} transactionId={}", clientId, transactionId);
                            status = VoteResultStatus.SignatureFailed;
                        } else {
                            status = addVoteAndHandleErrors(votingRecord, client, transactionId, packetSize, clientPacketResidual, encryptedData, voteDigest, System.currentTimeMillis());
                        }
                    } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                        log.error("acceptVote. verifySignature failed. clientId={} transactionId={} error={}", clientId, transactionId, e.getMessage());
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
            throw new InternalLogicException(String.format("Can not sign vote clientId=%s transactionId=%s", clientId, transactionId));
        }
        return new NodeVoteReceipt(inputMessage, now, status, receiptSign);
    }

    private VoteResultStatus addVoteAndHandleErrors(VotingRecord votingRecord, Client client, String transactionId, 
                                                                 BigDecimal packetSize, BigDecimal clientPacketResidual, 
                                                                String encryptedData, String voteDigest, long voteTimestamp) throws InternalLogicException {
        String encrypted = null;
        if (parentHolder != null) {
            try {
                encrypted = encryptToMasterNode(encryptedData, participantId, cryptoHelper.createSignature(MessageBuilder.buildMessage(encryptedData, participantId), privateKey));
            } catch (GeneralSecurityException | UnsupportedEncodingException | InternalLogicException e) {
                log.error("addVote. encrypt message to parent failed. voting={} client={} error={}", votingRecord.voting.getId(), client.getParticipantId(), e.getMessage());
                return VoteResultStatus.InternalError;
            }
        }
        VoteResultStatus status;
        synchronized (votingRecord) {
            status = checkVote(votingRecord, client, packetSize, clientPacketResidual, voteTimestamp);
            if (status == VoteResultStatus.OK) {
                log.debug("addVote. participantId={} votingId={} client={} packetSize={} oldTotalResidual={} oldClientResidual={} newClientPacketResidual={} newTotalResidual={}", 
                    participantId, votingRecord.voting.getId(), client.getParticipantId(), packetSize,
                    votingRecord.totalResidual, votingRecord.clientResidualsByClientId.get(client.getParticipantId()), 
                    clientPacketResidual, votingRecord.totalResidual.subtract(packetSize));
                votingRecord.clientResidualsByClientId.put(client.getParticipantId(), clientPacketResidual);
                votingRecord.totalResidual = votingRecord.totalResidual.subtract(packetSize);
                if (parentHolder != null) {
                    String signed = buildMessage(transactionId, votingRecord.voting.getId(), packetSize, participantId, votingRecord.totalResidual, encrypted, voteDigest);
                    String sign = null;
                    try {
                        sign = cryptoHelper.createSignature(signed, privateKey);
                    } catch (GeneralSecurityException | UnsupportedEncodingException  e) {
                        log.error("addVote. sign message to parent failed. voting={} client={} error={}", votingRecord.voting.getId(), client.getParticipantId(), e.getMessage());
                    }
                    if (sign != null) {
                        parentHolder.acceptVote(transactionId, votingRecord.voting.getId(), packetSize, participantId, votingRecord.totalResidual, encrypted, voteDigest, sign);
                    }
                }
            } else if (status == VoteResultStatus.TooLowResidual) {
                votingRecord.clientResidualsByClientId.put(client.getParticipantId(), clientPacketResidual);
                votingRecord.totalResidual = votingRecord.totalResidual.subtract(packetSize);
            }
        }
        if (status != VoteResultStatus.OK) {
            network.addVoteStatus(new VoteStatus(votingRecord.voting.getId(), transactionId, status, voteDigest, AssetsHolder.EMPTY_SIGNATURE));
        } 
        return status;
    }

    private VoteResultStatus checkVote(VotingRecord votingRecord, Client client, BigDecimal packetSize, BigDecimal clientPacketResidual, long timestamp) {
        if (votingRecord.voting.getBeginTimestamp() > timestamp) {
            log.warn("checkVote. Voting {} does not begin yet. client={}", votingRecord.voting.getId(), client.getParticipantId());
            return VoteResultStatus.IncorrectMessage;
        }
        if (votingRecord.voting.getEndTimestamp() < timestamp - VOTE_MAX_DELAY) {
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
        int compareResult = prevResidual.subtract(packetSize).compareTo(clientPacketResidual);
        if (compareResult < 0) {
            log.warn("checkVote. Too big clientPacketResidual {} (expected {}). Voting={} client={} packetSize={} prevResidual={}", 
                clientPacketResidual, prevResidual.subtract(packetSize), votingRecord.voting.getId(), client.getParticipantId(), packetSize, prevResidual);
            return VoteResultStatus.TooBigResidual;
        } else if (compareResult > 0)
            log.warn("checkVote. Too low clientPacketResidual {} (expected {}). Voting={} client={} packetSize={} prevResidual={}",
                clientPacketResidual, prevResidual.subtract(packetSize), votingRecord.voting.getId(), client.getParticipantId(), packetSize, prevResidual);
        
        return VoteResultStatus.OK;
    }

    @Override
    public Voting getVoting(String votingId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        return votingRecord == null ? null : votingRecord.voting;
    }

    @Override
    public Collection<Voting> getVotings() {
        return votingsById.values().stream().map(vr -> vr.voting).collect(Collectors.toList());
    }

    @Override
    public VoteResult getTotalVotingResult(String votingId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        return votingRecord == null ? null : votingRecord.totalResult;
    }

    @Override
    public Collection<VoteStatus> getVoteStatuses(String votingId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            return null;
        }
        synchronized (votingRecord) {
            return votingRecord.voteStatusesByMessageId.values();
        }
    }

    @Override
    public Collection<VoteResultAndStatus> getClientVotes(String votingId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            return null;
        }
        synchronized (votingRecord) {
            return votingRecord.clientResultsByClientId.values().stream().map(clientResult -> getClientVote(votingRecord, clientResult)).collect(Collectors.toList());
        }
    }

    @Override
    public VoteResultAndStatus getClientVote(String votingId, String clientId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            return null;
        }
        synchronized (votingRecord) {
            VoteResult clientResult = votingRecord.clientResultsByClientId.get(clientId);
            if (clientResult == null) {
                return null;
            }
            return getClientVote(votingRecord, clientResult);
        }
    }
    
    private VoteResultAndStatus getClientVote(VotingRecord votingRecord, VoteResult clientResult) {
        String messageId = votingRecord.clientResultMessageIdsByClientId.get(clientResult.getHolderId());
        return new VoteResultAndStatus(clientResult, messageId == null ? null : votingRecord.voteStatusesByMessageId.get(messageId));
    }

    @Override
    public ClientVoteReceipt addClientVote(VoteResult result, String signature) throws InternalLogicException {
        log.debug("addClientVote votingId={} ownerId={} packetSize={}", result.getVotingId(), result.getHolderId(), result.getPacketSize());
        VotingRecord votingRecord = votingsById.get(result.getVotingId());
        if (votingRecord == null) {
            throw new InternalLogicException(String.format("addClientVote. Voting not found. votingId=%s clientId=%s", result.getVotingId(), result.getHolderId()));
        }
        Client client;
        synchronized (votingRecord) {
            client = votingRecord.clients.get(result.getHolderId());
        }
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
        String tranId = network.addVote(result, serializedVote, signature, nodeSignature);
        String voteDigest;
        try {
            voteDigest = cryptoHelper.getDigest(serializedVote);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalLogicException("Can not calculate hash");
        }
        addVoteAndHandleErrors(votingRecord, client, tranId, result.getPacketSize(), client.getPacketSizeBySecurity().get(votingRecord.voting.getSecurity()).subtract(result.getPacketSize()),
            encryptToMasterNode(result.getHolderId(), signature), voteDigest, System.currentTimeMillis());
        synchronized (votingRecord) {
            votingRecord.clientResultsByClientId.put(result.getHolderId(), result);
            votingRecord.clientResultMessageIdsByClientId.put(result.getHolderId(), tranId);
        }
        
        long now = System.currentTimeMillis();
        String signedText = MessageBuilder.buildMessage(serializedVote, tranId, voteDigest, Long.toString(now));
        String receiptSign;
        try {
            receiptSign = cryptoHelper.createSignature(signedText, privateKey);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            throw new InternalLogicException("Can not sign vote");
        }
        if (stateSaver != null)
            stateSaver.accept(collectState());
        return new ClientVoteReceipt(serializedVote, tranId, voteDigest, now, receiptSign);
    }

    @Override
    public BigDecimal getClientPacketSize(String votingId, String clientId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            log.warn("getClientPacketSize. Voting not found {}. clientId={}", votingId, clientId);
            return null;
        }
        synchronized (votingRecord) {
            Client client = votingRecord.clients.get(clientId);
            if (client == null) {
                log.warn("getClientPacketSize. Client not found on voting begin. votingId={} clientId={}", votingId, clientId);
                return null;
            }
            return client.getPacketSizeBySecurity().get(votingRecord.voting.getSecurity());
        }
    }

    @Override
    public void addVoting(Voting voting) {
        VotingRecord votingRecord = CollectionsHelper.getOrAdd(votingsById, voting.getId(), VotingRecord::new);
        votingRecord.voting = voting;
        setVotingClients(votingRecord);
    }

    private void setVotingClients(VotingRecord votingRecord) {
        synchronized (votingRecord) {
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
        }
        log.debug("setVotingClients. votingId={} participantId={} totalResidual={}", votingRecord.voting.getId(), participantId, votingRecord.totalResidual);
    }

    @Override
    public void addVotingTotalResult(VoteResult result) {
        VotingRecord votingRecord = votingsById.get(result.getVotingId());
        if (votingRecord == null) {
            log.warn("addVotingTotalResult. Voting not found {}", result.getVotingId());
            return;
        }
        votingRecord.totalResult = result;
    }

    @Override
    public void addVoteStatus(VoteStatus status) {
        VotingRecord votingRecord = votingsById.get(status.getVotingId());
        if (votingRecord == null) {
            log.warn("addVoteStatus. Voting not found {}", status.getVotingId());
            return;
        }
        synchronized (votingRecord) {
            votingRecord.voteStatusesByMessageId.put(status.getMessageId(), status);
        }
    }

    @Override
    public synchronized void addVote(VoteResult result, String messageId, String serializedResult) {
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


    private void loadState(String state) {
        String[] terms = MessageBuilder.splitMessage(state);
        votingsById.clear();
        for(int i = 0; i < terms.length-1; i+=2) {
            votingsById.put(terms[i], new VotingRecord(terms[i+1]));
        }
    }

    private String collectState() {
        List<String> stateParts = new ArrayList<>();
        for(Map.Entry<String, VotingRecord> recordEntry : votingsById.entrySet()) {
            synchronized (recordEntry.getValue()) {
                stateParts.add(recordEntry.getKey());
                stateParts.add(recordEntry.getValue().toString());
            }
        }
        return MessageBuilder.buildMessage(stateParts.toArray(new String[stateParts.size()]));
    }
}
