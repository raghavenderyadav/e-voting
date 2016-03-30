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

package uk.dsxt.voting.client;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.domain.dataModel.NodeVoteReceipt;
import uk.dsxt.voting.common.domain.nodes.VoteAcceptor;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.math.BigDecimal;

@Log4j2
@Path("/holderAPI")
public class HolderApiResource {
    private final VoteAcceptor node;

    public HolderApiResource(VoteAcceptor node) {
        this.node = node;
    }

    @POST
    @Path("/acceptVote")
    @Produces("application/json")
    public NodeVoteReceipt acceptVote(@FormParam("transactionId") String transactionId, @FormParam("votingId") String votingId, @FormParam("packetSize") String packetSize,
                                      @FormParam("clientId") String clientId, @FormParam("clientPacketResidual") String clientPacketResidual,
                                      @FormParam("encryptedData") String encryptedData, @FormParam("voteDigest") String voteDigest, @FormParam("clientSignature") String clientSignature) {
        try {
            return node.acceptVote(transactionId, votingId, new BigDecimal(packetSize), clientId, new BigDecimal(clientPacketResidual), encryptedData, voteDigest, clientSignature);
        } catch (Exception e) {
            log.error("acceptVote fails", e);
            return null;
        }
    }
}
