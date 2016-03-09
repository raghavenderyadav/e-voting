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
import uk.dsxt.voting.client.auth.AuthManager;
import uk.dsxt.voting.client.datamodel.*;
import uk.dsxt.voting.client.web.VotingAPI;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.function.Supplier;

@Log4j2
@Path("/api")
public class VotingApiResource implements VotingAPI {

    private final ClientManager manager;
    private final AuthManager authManager;

    public VotingApiResource(ClientManager manager, AuthManager authManager) {
        this.manager = manager;
        this.authManager = authManager;
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
        return authManager.login(login, password);
    }

    @POST
    @Path("/logout")
    @Produces("application/json")
    public boolean logout(@FormParam("cookie") String cookie) {
        log.debug("logout method called. cookie={};", cookie);
        return authManager.logout(cookie);
    }

    @POST
    @Path("/votings")
    @Produces("application/json")
    public VotingWeb[] getVotings(@FormParam("cookie") String cookie) {
        return execute("getVotings", "", manager::getVotings);
    }

    @POST
    @Path("/getVoting")
    @Produces("application/json")
    public VotingInfoWeb getVoting(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        return execute("getVoting", String.format("votingId=%s", votingId), () -> manager.getVoting(votingId));
    }

    @POST
    @Path("/vote")
    @Produces("application/json")
    public boolean vote(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId, @FormParam("votingChoice") String votingChoice) {
        // TODO Move cookie checks into execute method.
        final LoggedUser loggedUser = authManager.tryGetLoggedUser(cookie);
        if (loggedUser == null || loggedUser.getClientId().isEmpty()) {
            return false;
        }
        return execute("vote", String.format("votingId=%s, votingChoice=%s", votingId, votingChoice), () -> manager.vote(votingId, loggedUser.getClientId(), votingChoice));
    }

    @POST
    @Path("/votingResults")
    @Produces("application/json")
    public QuestionWeb[] votingResults(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        // TODO Move cookie checks into execute method.
        final LoggedUser loggedUser = authManager.tryGetLoggedUser(cookie);
        if (loggedUser == null || loggedUser.getClientId().isEmpty()) {
            return new QuestionWeb[0];
        }
        return execute("votingResults", String.format("votingId=%s", votingId), () -> manager.votingResults(votingId, loggedUser.getClientId()));
    }

    @POST
    @Path("/getTime")
    @Produces("application/json")
    public long getTime(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        return execute("getTime", String.format("votingId=%s", votingId), () -> manager.getTime(votingId));
    }

    @POST
    @Path("/getConfirmedClientVotes")
    @Produces("application/json")
    public VoteResultWeb[] getConfirmedClientVotes(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        return execute("getConfirmedClientVotes", String.format("votingId=%s", votingId), () -> manager.getConfirmedClientVotes(votingId));
    }
}
