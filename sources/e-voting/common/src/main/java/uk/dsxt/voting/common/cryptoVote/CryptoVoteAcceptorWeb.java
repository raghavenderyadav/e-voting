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

package uk.dsxt.voting.common.cryptoVote;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jetty.util.ArrayQueue;
import org.eclipse.jetty.util.ConcurrentArrayQueue;
import uk.dsxt.voting.common.demo.NetworkConnectorDemo;
import uk.dsxt.voting.common.domain.dataModel.NodeVoteReceipt;
import uk.dsxt.voting.common.domain.dataModel.VoteResultStatus;
import uk.dsxt.voting.common.domain.nodes.VoteAcceptor;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.web.HttpHelper;
import uk.dsxt.voting.common.utils.web.RequestType;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class CryptoVoteAcceptorWeb extends NetworkConnectorDemo implements VoteAcceptor {

    private final static String ACCEPT_VOTE_URL_PART = "/acceptVote";

    private final HttpHelper httpHelper;

    private final String acceptVoteUrl;
    
    private final ObjectMapper mapper = new ObjectMapper();
    
    private final Queue<Map<String, String>> unsentVoteMessages = new ConcurrentArrayQueue<>(1000);
    
    private final File receiptsFile;

    private final Thread[] senders;

    public CryptoVoteAcceptorWeb(String baseUrl, int connectionTimeout, int readTimeout, String receiptsFilePath, int threadCount) {
        super();
        acceptVoteUrl = String.format("%s%s", baseUrl, ACCEPT_VOTE_URL_PART);
        httpHelper = new HttpHelper(connectionTimeout, readTimeout);
        receiptsFile = receiptsFilePath == null || receiptsFilePath.isEmpty() ? null : new File(receiptsFilePath);
        senders = new Thread[threadCount];
        for(int i = 0; i < threadCount; i++) {
            senders[i] = new Thread(this::sendVotes);
            senders[i].setPriority(Thread.MAX_PRIORITY);
            senders[i].start();
        }
    }

    @Override
    public NodeVoteReceipt acceptVote(String transactionId, String votingId, BigDecimal packetSize, String clientId, BigDecimal clientPacketResidual, String encryptedData, String voteDigest, String clientSignature) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("transactionId", transactionId);
        parameters.put("votingId", votingId);
        parameters.put("packetSize", packetSize.toPlainString());
        parameters.put("clientId", clientId);
        parameters.put("clientPacketResidual", clientPacketResidual.toPlainString());
        parameters.put("encryptedData", encryptedData);
        parameters.put("voteDigest", voteDigest);
        parameters.put("clientSignature", clientSignature);
        unsentVoteMessages.add(parameters);
        return null;
    }
    
    private void sendVotes() {
        while(true) {
            sendNextVote();
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                log.info("sendVotes interrupted");
                return;
            }
        }
    }
    
    private void sendNextVote() {
        if (!isNetworkOn)
            return;
        Map<String, String> parameters = unsentVoteMessages.poll();
        int queueSize = unsentVoteMessages.size();
        if (parameters == null)
            return;
        
        String result;
        try {
            result = httpHelper.request(acceptVoteUrl, parameters, RequestType.POST);
        } catch (IOException e) {
            log.warn("sendNextVote failed. url={} error={}", acceptVoteUrl, e);
            unsentVoteMessages.add(parameters);
            return;
        } catch (InternalLogicException e) {
            log.error("sendNextVote failed. url={}", acceptVoteUrl, e);
            unsentVoteMessages.add(parameters);
            return;
        }
        if (result == null || result.isEmpty()) {
            log.error("sendNextVote. result == null. url={}", acceptVoteUrl);
            unsentVoteMessages.add(parameters);
            return;
        }
        
        if (receiptsFile != null) {
            synchronized (receiptsFile) {
                try {
                    Files.write(receiptsFile.toPath(), Collections.singletonList(result), Charset.forName("utf-8"), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                } catch (IOException e) {
                    log.warn("sendNextVote. Couldn't save result to file: {}. result='{}' error={}", receiptsFile.getAbsolutePath(), result, e.getMessage());
                }
            }
        }
        try {
            NodeVoteReceipt receipt = mapper.readValue(result, NodeVoteReceipt.class);
            log.debug("sendNextVote. Vote sent, receipt.status={} clientPacketResidual={} packetSize={} queueSize={}",
                receipt.getStatus(), parameters.get("clientPacketResidual"), parameters.get("packetSize"), queueSize);
        } catch (IOException e) {
            log.error("sendNextVote. can not read receipt {}. error={}", result, e.getMessage());
        }
    }
}
