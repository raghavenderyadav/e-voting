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
import uk.dsxt.voting.client.datamodel.*;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.function.Supplier;

@Log4j2
@Path("/api")
public class VotingApiResource {
    private final ClientManager manager;

    public VotingApiResource(ClientManager manager) {
        this.manager = manager;
    }

    private <T> T execute(String name, String params, Supplier<T> request) {
        try {
            log.debug("{} called. params: [{}]", name, params);
            return request.get();
        } catch (Exception ex) {
            log.error("{} failed. params: [{}]", name, params, ex);
            return null;
        }
    }

    @POST
    @Path("/login")
    @Produces("application/json")
    public LoginAnswerWeb login(@FormParam("login") String login, @FormParam("password") String password) {
        log.debug("login method called. login={};", login);
        //TODO: implement
        return null;

    }

    @POST
    @Path("/logout")
    @Produces("application/json")
    public boolean logout(@FormParam("cookie") String cookie) {
        log.debug("logout method called. cookie={};", cookie);
        //TODO: implement
        return true;
    }

    @POST
    @Path("/votings")
    @Produces("application/json")
    public VotingWeb[] getVotings() {
        return execute("getVotings", "", manager::getVotings);
    }

    @POST
    @Path("/getVoting")
    @Produces("application/json")
    public VotingInfoWeb getVoting(@FormParam("votingId") String votingId) {
        return execute("getVoting", String.format("votingId=%s", votingId), () -> manager.getVoting(votingId));
    }

    @POST
    @Path("/vote")
    @Produces("application/json")
    public boolean vote(@FormParam("votingId") String votingId, @FormParam("votingChoice") String votingChoice) {
        String clientId = ""; // TODO Get it from auth.
        return execute("vote", String.format("votingId=%s, votingChoice=%s", votingId, votingChoice), () -> manager.vote(votingId, clientId, votingChoice));
    }

    @POST
    @Path("/votingResults")
    @Produces("application/json")
    public QuestionWeb[] votingResults(@FormParam("votingId") String votingId) {
        String clientId = ""; // TODO Get client ID from auth.
        return execute("votingResults", String.format("votingId=%s", votingId), () -> manager.votingResults(votingId, clientId));
    }

    @POST
    @Path("/getTime")
    @Produces("application/json")
    public long getTime(@FormParam("votingId") String votingId) {
        return execute("getTime", String.format("votingId=%s", votingId), () -> manager.getTime(votingId));
    }

    @POST
    @Path("/getConfirmedClientVotes")
    @Produces("application/json")
    public VoteResultWeb[] getConfirmedClientVotes(@FormParam("votingId") String votingId) {
        return execute("getConfirmedClientVotes", String.format("votingId=%s", votingId), () -> manager.getConfirmedClientVotes(votingId));
    }
}
