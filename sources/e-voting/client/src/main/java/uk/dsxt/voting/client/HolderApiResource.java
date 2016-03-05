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
import uk.dsxt.voting.common.cryptoVote.CryptoVoteAcceptor;
import uk.dsxt.voting.common.cryptoVote.CryptoVoteAcceptorWeb;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

@Log4j2
@Path("/holderAPI")
public class HolderApiResource {
    private final CryptoVoteAcceptor cryptoVoteAcceptor;

    public HolderApiResource(CryptoVoteAcceptor cryptoVoteAcceptor) {
        this.cryptoVoteAcceptor = cryptoVoteAcceptor;
    }

    /*private <T> T execute(String name, Supplier<T> request) {
        try {
            return request.get();
        } catch (Exception ex) {
            log.error("{} failed", name, ex);
            manager.stop();
            return null;
        }
    }*/

    @POST
    @Path("/acceptVote")
    public void acceptVote(String newResultMessage, String clientId, String joinedSignatures) {
        try {
            String[] signatures = joinedSignatures.split(CryptoVoteAcceptorWeb.SIGNATURE_SEPARATOR);
            cryptoVoteAcceptor.acceptVote(newResultMessage, clientId, Arrays.stream(signatures).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("acceptVote fails", e);
        }
    }
}
