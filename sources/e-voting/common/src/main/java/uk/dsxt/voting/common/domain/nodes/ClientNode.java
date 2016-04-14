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
import lombok.extern.log4j.Log4j2;
import org.joda.time.Instant;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class ClientNode implements AssetsHolder, NetworkClient {

    protected VoteAcceptor parentHolder;

    protected NetworkMessagesSender network;

    private final String participantId;

    protected final Map<String, PublicKey> participantKeysById;

    protected final PrivateKey privateKey;

    protected final CryptoHelper cryptoHelper;

    protected final MessagesSerializer messagesSerializer;

    private final PublicKey masterNodePublicKey;

    private final SortedMap<Long, Map<String, Client>> clientsByIdByTimestamp = new TreeMap<>();

    private final Map<String, VotingRecord> votingsById = new ConcurrentHashMap<>();

    private final Consumer<String> stateSaver;
    
    private final static long VOTE_MAX_DELAY = 60000;

    private final Map<String, OwnerRecord> ownerRecordsByMessageId = new HashMap<>();
    
    private final AtomicLong acceptingVotes = new AtomicLong();
    private final AtomicLong acceptedVotes = new AtomicLong();
    private final AtomicLong incorrectVotes = new AtomicLong();
    private final AtomicLong addingClientVotes = new AtomicLong();
    private final AtomicLong addedClientVotes = new AtomicLong();

    public ClientNode(String participantId, MessagesSerializer messagesSerializer, CryptoHelper cryptoProvider, Map<String, PublicKey> participantKeysById, PrivateKey privateKey,
                      VoteAcceptor parentHolder, String state, Consumer<String> stateSaver)
        throws InternalLogicException, GeneralSecurityException {
        this.participantId = participantId;
        this.messagesSerializer = messagesSerializer;
        this.privateKey = privateKey;
        this.cryptoHelper = cryptoProvider;
        this.participantKeysById = participantKeysById;
        this.parentHolder = parentHolder;
        this.stateSaver = stateSaver;
        masterNodePublicKey = participantKeysById.get(MasterNode.MASTER_HOLDER_ID);
        if (masterNodePublicKey == null)
            throw new InternalLogicException(String.format("Master node %s has no public key", MasterNode.MASTER_HOLDER_ID));
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
        acceptingVotes.incrementAndGet();
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
                PublicKey participantKey = participantKeysById.get(clientId);
                if (participantKey == null) {
                    log.error("acceptVote. participant not found or has no public key. clientId={} transactionId={}", clientId, transactionId);
                    status = VoteResultStatus.IncorrectMessage;
                } else {
                    try {
                        if (!cryptoHelper.verifySignature(inputMessage, clientSignature, participantKey)) {
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
        if (status != VoteResultStatus.OK)
            incorrectVotes.incrementAndGet();
        long now = System.currentTimeMillis();
        String signedText = MessageBuilder.buildMessage(inputMessage, Long.toString(now), status.toString());
        String receiptSign;
        try {
            receiptSign = cryptoHelper.createSignature(signedText, privateKey);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            throw new InternalLogicException(String.format("Can not sign vote clientId=%s transactionId=%s", clientId, transactionId));
        }
        acceptedVotes.incrementAndGet();
        log.debug("acceptVote. votingId={} clientId={} packetSize={} status={} acceptingVotes={} acceptedVotes={} incorrectVotes={}",
            votingId, clientId, packetSize, status, acceptingVotes.get(), acceptedVotes.get(), incorrectVotes.get());
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
                Map<BigDecimal, BigDecimal> ranges = CollectionsHelper.getOrAdd(votingRecord.clientVoteRangesByClientId, client.getParticipantId(), TreeMap<BigDecimal, BigDecimal>::new);
                ranges.put(clientPacketResidual, clientPacketResidual.add(packetSize));
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
        Map<BigDecimal, BigDecimal> ranges = votingRecord.clientVoteRangesByClientId.get(client.getParticipantId());
        BigDecimal totalAmount = client.getPacketSizeBySecurity().get(votingRecord.voting.getSecurity());
        BigDecimal upperBound = clientPacketResidual.add(packetSize);
        if (upperBound.compareTo(totalAmount) > 0) {
            log.warn("checkVote. Too big clientPacketResidual {} (expected {}). Voting={} client={} packetSize={} totalAmount={}",
                clientPacketResidual, totalAmount.subtract(packetSize), votingRecord.voting.getId(), client.getParticipantId(), packetSize, totalAmount);
            return VoteResultStatus.TooBigResidual;
        }
        if (ranges != null) {
            for(Map.Entry<BigDecimal, BigDecimal> usedRange : ranges.entrySet()) {
                if (clientPacketResidual.compareTo(usedRange.getValue()) >= 0)
                    break;
                if (upperBound.compareTo(usedRange.getKey()) > 0) {
                    log.warn("checkVote. Intersected clientVoteRange {}-{} and range {}-{}. Voting={} client={}",
                        usedRange.getKey(), usedRange.getValue(), clientPacketResidual, upperBound, votingRecord.voting.getId(), client.getParticipantId());
                    return VoteResultStatus.TooBigResidual;
                }
            }
        }
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
            return votingRecord.ownerRecordsByClientId.values().stream().map(cr -> cr.resultAndStatus).collect(Collectors.toList());
        }
    }

    @Override
    public VoteResultAndStatus getClientVote(String votingId, String clientId) {
        VotingRecord votingRecord = votingsById.get(votingId);
        if (votingRecord == null) {
            return null;
        }
        synchronized (votingRecord) {
            OwnerRecord ownerRecord = votingRecord.ownerRecordsByClientId.get(clientId);
            return ownerRecord == null ? null : ownerRecord.resultAndStatus;
        }
    }

    @Override
    public void addClientVote(VoteResult result, String signature) throws InternalLogicException {
        addingClientVotes.incrementAndGet();
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
        String serializedVote = messagesSerializer.serialize(result, votingRecord.voting);
        String voteDigest;
        try {
            voteDigest = cryptoHelper.getDigest(serializedVote);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalLogicException("Can not calculate hash");
        }

        String voteMessageId;
        try {
            voteMessageId = network.addVote(result, serializedVote, signature);
        } catch (InternalLogicException e) {
            log.error("addClientVote. sendVote failed. votingId={} ownerId={} error={}",
                result.getVotingId(), result.getHolderId(), e.getMessage());
            return;
        }
        long now = System.currentTimeMillis();
        String signedText = MessageBuilder.buildMessage(serializedVote, voteMessageId, voteDigest, Long.toString(now));
        String receiptSign = null;
        try {
            receiptSign = cryptoHelper.createSignature(signedText, privateKey);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            log.error("addClientVote. Can not sign vote. ownerId={} error={}", result.getHolderId(), e.getMessage());
        }
        String encryptedData = null;
        try {
            encryptedData = encryptToMasterNode(result.getHolderId(), signature);
        } catch (InternalLogicException e) {
            log.error("addClientVote. Can not encrypt vote data. ownerId={} error={}", result.getHolderId(), e.getMessage());
        }
        ClientVoteReceipt receipt = new ClientVoteReceipt(serializedVote, voteMessageId, voteDigest, now, receiptSign);
        OwnerRecord ownerRecord = new OwnerRecord(result, serializedVote, signature, voteDigest, voteMessageId, receipt);
 
        try {
            addVoteAndHandleErrors(votingRecord, client, voteMessageId, result.getPacketSize(),
                client.getPacketSizeBySecurity().get(votingRecord.voting.getSecurity()).subtract(result.getPacketSize()),
                encryptedData, voteDigest, System.currentTimeMillis());
        } catch (InternalLogicException e) {
            log.error("addClientVote. Can not add vote. ownerId={} error={}", result.getHolderId(), e.getMessage());
        }
        synchronized (ownerRecordsByMessageId) {
            ownerRecordsByMessageId.put(ownerRecord.voteMessageId, ownerRecord);
        }
        synchronized (votingRecord) {
            votingRecord.ownerRecordsByClientId.put(result.getHolderId(), ownerRecord);
        }
        if (stateSaver != null)
            stateSaver.accept(collectState());
        addedClientVotes.incrementAndGet();
        log.debug("addClientVote. Vote added. ownerId={} votingId={} messageId={} packetSize={} addingClientVotes={} addedClientVotes={}", 
            result.getHolderId(), result.getVotingId(), voteMessageId, result.getPacketSize(), addingClientVotes.get(), addedClientVotes.get());
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
                    if (votingRecord.ownerRecordsByClientId.size() == 0)
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
    public void addVoteStatus(VoteStatus status, String messageId, boolean isCommitted, boolean isSelf) {
        VotingRecord votingRecord = votingsById.get(status.getVotingId());
        if (votingRecord == null) {
            log.warn("addVoteStatus. Voting not found {}", status.getVotingId());
            return;
        }
        synchronized (votingRecord) {
            votingRecord.voteStatusesByMessageId.put(status.getMessageId(), status);
        }
        OwnerRecord ownerRecord;
        synchronized (ownerRecordsByMessageId) {
            ownerRecord = ownerRecordsByMessageId.remove(status.getMessageId());
        }
        if (ownerRecord != null) {
            synchronized (ownerRecord) {
                ownerRecord.resultAndStatus.setStatus(status);
            }
            log.debug("addVoteStatus. messageId={} ownerId={} status={} statusMessageId={}", 
                status.getMessageId(), ownerRecord.resultAndStatus.getResult().getHolderId(), status.getStatus(), messageId);
            if (stateSaver != null)
                stateSaver.accept(collectState());
        }
    }

    @Override
    public void addVoteToMaster(VoteResult result, String messageId, String serializedResult, boolean isCommitted, boolean isSelf) {
    }

    @Override
    public void notifyVote(String messageId, boolean isCommitted, boolean isSelf) {
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
            VotingRecord votingRecord = new VotingRecord(terms[i+1]);
            votingsById.put(terms[i], votingRecord);
            for(OwnerRecord ownerRecord : votingRecord.ownerRecordsByClientId.values()) {
                if (ownerRecord.voteMessageId != null) {
                    ownerRecordsByMessageId.put(ownerRecord.voteMessageId, ownerRecord);
                }
            }
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
