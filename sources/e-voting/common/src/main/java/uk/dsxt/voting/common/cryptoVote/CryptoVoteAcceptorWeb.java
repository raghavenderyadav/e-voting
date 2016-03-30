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
import uk.dsxt.voting.common.domain.dataModel.NodeVoteReceipt;
import uk.dsxt.voting.common.domain.nodes.VoteAcceptor;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.web.HttpHelper;
import uk.dsxt.voting.common.utils.web.RequestType;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class CryptoVoteAcceptorWeb implements VoteAcceptor {

    private final static String ACCEPT_VOTE_URL_PART = "/acceptVote";

    private final HttpHelper httpHelper;

    private final String acceptVoteUrl;
    
    private final ObjectMapper mapper = new ObjectMapper();

    public CryptoVoteAcceptorWeb(String baseUrl, int connectionTimeout, int readTimeout) {
        acceptVoteUrl = String.format("%s%s", baseUrl, ACCEPT_VOTE_URL_PART);
        httpHelper = new HttpHelper(connectionTimeout, readTimeout);
    }

    private String execute(String name, String url, Map<String, String> parameters) {
        try {
            return httpHelper.request(url, parameters, RequestType.POST);
        } catch (IOException e) {
            log.error("{} failed. url={} error={}", name, url, e);
        } catch (InternalLogicException e) {
            log.error("{} failed. url={}", name, url, e);
        }
        return null;
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
        String result = execute("acceptVote", acceptVoteUrl, parameters);
        try {
            NodeVoteReceipt receipt = mapper.readValue(result, NodeVoteReceipt.class);
            return receipt;
        } catch (IOException e) {
            log.error("acceptVote. can not read receipt. error={}", e.getMessage());
            return null;
        }
    }
}
