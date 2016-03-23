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
import uk.dsxt.voting.client.datamodel.APIException;
import uk.dsxt.voting.client.datamodel.LoggedUser;
import uk.dsxt.voting.client.datamodel.RequestResult;
import uk.dsxt.voting.client.datamodel.UserRole;
import uk.dsxt.voting.client.web.VotingAPI;
import uk.dsxt.voting.common.utils.InternalLogicException;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Log4j2
@Path("/api")
public class VotingApiResource implements VotingAPI {

    private final ClientManager manager;
    private final AuthManager authManager;

    public VotingApiResource(ClientManager manager, AuthManager authManager) {
        this.manager = manager;
        this.authManager = authManager;
    }

    @FunctionalInterface
    private interface SimpleRequest {
        RequestResult get() throws InternalLogicException;
    }

    @FunctionalInterface
    private interface ClientIdRequest {
        RequestResult get(String clientId) throws InternalLogicException;
    }

    @FunctionalInterface
    private interface ClientRequest {
        RequestResult get(String clientId, UserRole role) throws InternalLogicException;
    }

    private RequestResult execute(String name, String params, SimpleRequest request) {
        try {
            log.debug("{} called. params: [{}]", name, params);
            return request.get();
        } catch (Exception ex) {
            log.error("{} failed. params: [{}]", name, params, ex);
            return new RequestResult<>(APIException.UNKNOWN_EXCEPTION);
        }
    }

    private RequestResult executeClient(String cookie, String name, String params, ClientRequest request) {
        return execute(name, params, () -> {
            final LoggedUser loggedUser = authManager.tryGetLoggedUser(cookie);
            if (loggedUser == null || loggedUser.getClientId().isEmpty()) {
                log.warn("{} failed. Incorrect cookie: {}", name, cookie);
                return new RequestResult<>(APIException.WRONG_COOKIE);
            }
            return request.get(loggedUser.getClientId(), loggedUser.getRole());
        });
    }

    private RequestResult executeClientId(String cookie, String name, String params, ClientIdRequest request) {
        return executeClient(cookie, name, params, (clientId, role) -> request.get(clientId));
    }

    private RequestResult executeClientWithRole(String cookie, String name, String params, UserRole expectedRole, ClientIdRequest request) {
        return executeClient(cookie, name, params, (clientId, userRole) -> {
            if (userRole != expectedRole) {
                log.warn("{} failed. Incorrect rights {} for client id {}. Expected right {}", name, userRole, clientId, expectedRole);
                return new RequestResult<>(APIException.INCORRECT_RIGHTS);
            }
            return request.get(clientId);
        });
    }

    @POST
    @Path("/login")
    @Produces("application/json")
    public RequestResult login(@FormParam("login") String login, @FormParam("password") String password) {
        return execute("login", String.format("login=%s", login), () -> authManager.login(login, password));
    }

    @POST
    @Path("/logout")
    @Produces("application/json")
    public RequestResult logout(@FormParam("cookie") String cookie) {
        return executeClientId(cookie, "logout", "", (clientId) -> authManager.logout(cookie));
    }

    @POST
    @Path("/votings")
    @Produces("application/json")
    public RequestResult getVotings(@FormParam("cookie") String cookie) {
        return executeClientId(cookie, "getVotings", "", manager::getVotings);
    }

    @POST
    @Path("/getVoting")
    @Produces("application/json")
    public RequestResult getVoting(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        return executeClientWithRole(cookie, "getVoting", String.format("votingId=%s", votingId), UserRole.VOTER, (clientId) -> manager.getVoting(votingId, clientId));
    }

    @POST
    @Path("/vote")
    @Produces("application/json")
    public RequestResult vote(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId, @FormParam("votingChoice") String votingChoice) {
        return executeClientWithRole(cookie, "vote", String.format("votingId=%s, votingChoice=%s", votingId, votingChoice), UserRole.VOTER,
            (clientId) -> manager.vote(votingId, clientId, votingChoice));
    }

    @POST
    @Path("/signVote")
    @Produces("application/json")
    public RequestResult signVote(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId, @FormParam("isSign") Boolean isSign, @FormParam("signature") String signature) {
        return executeClientWithRole(cookie, "signVote", String.format("votingId=%s, signature=%s, isSign=%s", votingId, signature, isSign), UserRole.VOTER,
            (clientId) -> manager.signVote(votingId, clientId, isSign, signature));
    }

    @POST
    @Path("/votingResults")
    @Produces("application/json")
    public RequestResult votingResults(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        return executeClientId(cookie, "votingResults", String.format("votingId=%s", votingId), (clientId) -> manager.votingResults(votingId, clientId));
    }

    @POST
    @Path("/getTime")
    @Produces("application/json")
    public RequestResult getTime(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        return executeClientId(cookie, "getTime", String.format("votingId=%s", votingId), (clientId) -> manager.getTime(votingId));
    }

    @POST
    @Path("/getAllVoteStatuses")
    @Produces("application/json")
    public RequestResult getAllVoteStatuses(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        return executeClientId(cookie, "getAllClientVotes", String.format("votingId=%s", votingId), (clientId) -> manager.getAllVoteStatuses(votingId));
    }

    @POST
    @Path("/votingTotalResults")
    @Produces("application/json")
    public RequestResult votingTotalResults(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        return executeClientId(cookie, "votingTotalResults", String.format("votingId=%s", votingId), (clientId) -> manager.votingTotalResults(votingId));
    }
}
