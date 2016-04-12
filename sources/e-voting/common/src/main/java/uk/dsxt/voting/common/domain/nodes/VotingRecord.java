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

import uk.dsxt.voting.common.domain.dataModel.Client;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.VoteStatus;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.utils.MessageBuilder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

class VotingRecord {
    Voting voting;
    VoteResult totalResult;
    Map<String, Client> clients = new HashMap<>();
    BigDecimal totalResidual = BigDecimal.ZERO;
    Map<String, OwnerRecord> ownerRecordsByClientId = new HashMap<>();
    Map<String, BigDecimal> clientResidualsByClientId = new HashMap<>();
    Map<String, VoteStatus> voteStatusesByMessageId = new HashMap<>();

    public VotingRecord() {
    }
    
    public String toString() {
        return MessageBuilder.buildMessage(totalResidual.toPlainString(), serialize(ownerRecordsByClientId), serialize(clientResidualsByClientId));
    }
    
    public VotingRecord(String string) {
        String[] terms = MessageBuilder.splitMessage(string);
        totalResidual = new BigDecimal(terms[0]);
        ownerRecordsByClientId = deserialize(terms[1], OwnerRecord::new);
        clientResidualsByClientId = deserialize(terms[2], BigDecimal::new);
    }
    
    private <T> String serialize(Map<String, T> entries) {
        StringBuffer sb = new StringBuffer();
        for(Map.Entry<String, T> entry : entries.entrySet()) {
            sb.append(entry.getKey());
            sb.append(';');
            sb.append(entry.getValue());
            sb.append(';');
        }
        return sb.toString();
    }

    private <T> Map<String, T> deserialize(String s, Function<String, T> constructor) {
        String[] terms = s.split(";");
        HashMap<String, T> result = new HashMap<>();
        for(int i = 0; i < terms.length-1; i+=2) {
            result.put(terms[i], constructor.apply(terms[i+1]));            
        }
        return result;
    }
}
