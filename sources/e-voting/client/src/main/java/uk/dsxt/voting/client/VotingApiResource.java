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
import uk.dsxt.voting.client.datamodel.QuestionWeb;
import uk.dsxt.voting.client.datamodel.VotingInfoWeb;
import uk.dsxt.voting.client.datamodel.VotingWeb;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Log4j2
@Path("/api")
public class VotingApiResource {
    private final ClientManager manager;

    public VotingApiResource(ClientManager manager) {
        this.manager = manager;
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
    @Path("/votings")
    @Produces("application/json")
    public VotingWeb[] getVotings() {
        return manager.getVotings();
    }

    @POST
    @Path("/getVoting")
    @Produces("application/json")
    public VotingInfoWeb getVoting(@FormParam("votingId") String votingId) {
        return manager.getVoting(votingId);
    }

    @POST
    @Path("/vote")
    @Produces("application/json")
    public QuestionWeb[] vote(@FormParam("votingId") String votingId, @FormParam("votes") String votes) {
        return manager.vote(votingId, votes);
    }

    @POST
    @Path("/votingResults")
    @Produces("application/json")
    public QuestionWeb[] votingResults(@FormParam("votingId") String votingId) {
        return new QuestionWeb[0];
    }


}
