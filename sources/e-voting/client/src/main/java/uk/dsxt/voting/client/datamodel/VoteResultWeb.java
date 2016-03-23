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

package uk.dsxt.voting.client.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.VoteResultAndStatus;
import uk.dsxt.voting.common.domain.dataModel.VoteResultStatus;

import java.math.BigDecimal;

@Value
public class VoteResultWeb {
    String votingId;
    String votingName;
    String clientId;
    String clientName;
    BigDecimal packetSize;
    VoteResultStatus status;
    String messageId;
    // TODO Add answers for each question

    @JsonCreator
    public VoteResultWeb(@JsonProperty("votingId") String votingId, @JsonProperty("votingName") String votingName,
                         @JsonProperty("clientId") String clientId, @JsonProperty("clientName") String clientName,
                         @JsonProperty("packetSize") BigDecimal packetSize, 
                         @JsonProperty("status") VoteResultStatus status, @JsonProperty("messageId") String messageId) {
        this.votingId = votingId;
        this.votingName = votingName;
        this.clientId = clientId;
        this.clientName = clientName;
        this.packetSize = packetSize;
        this.status = status;
        this.messageId = messageId;
    }

    public VoteResultWeb(VoteResultAndStatus vr) {
        this.votingId = vr.getResult().getVotingId();
        this.votingName = ""; // TODO Get votingName from other sources.
        this.clientId = vr.getResult().getHolderId();
        this.clientName = ""; // TODO Get clientName from other sources.
        this.packetSize = vr.getResult().getPacketSize();
        this.status = vr.getStatus() == null ? null : vr.getStatus().getStatus();
        this.messageId = vr.getStatus() == null ? null : vr.getStatus().getMessageId();
    }
}
