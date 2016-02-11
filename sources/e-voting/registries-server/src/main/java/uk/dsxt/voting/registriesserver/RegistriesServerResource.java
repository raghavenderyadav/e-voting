/******************************************************************************
 * e-voting system                                                            *
 * Copyright (C) 2016 DSX Technologies Limited.                               *
 *                                                                            *
 * This program is free software; you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation; either version 2 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied                         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * You can find copy of the GNU General Public License in LICENSE.txt file    *
 * at the top-level directory of this distribution.                           *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package uk.dsxt.voting.registriesserver;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.datamodel.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Log4j2
@Path("/voting-api")
public class RegistriesServerResource implements uk.dsxt.voting.common.networking.RegistriesServer {
    public static final String ERROR = "Unable to obtain data";

    private final RegistriesServerManager manager;

    public RegistriesServerResource(RegistriesServerManager manager) {
        this.manager = manager;
    }

    @FunctionalInterface
    public interface Request<T> {
        T[] get();
    }

    private <T> RequestResult<T> execute(String name, Request<T> request) {
        try {
            return new RequestResult<>(request.get());
        } catch (Exception ex) {
            log.error(String.format("%s failed", name), ex);
            return new RequestResult<>(ERROR);
        }
    }

    @Override
    @GET
    @Path("/holdings")
    @Produces("application/json")
    public RequestResult<Holding> getHoldings() {
        return execute("getHoldings", manager::getHoldings);
    }

    @Override
    @GET
    @Path("/participants")
    @Produces("application/json")
    public RequestResult<Participant> getParticipants() {
        return execute("getParticipants", manager::getParticipants);
    }

    @Override
    @GET
    @Path("/votings")
    @Produces("application/json")
    public RequestResult<Voting> getVotings() {
        return execute("getVotings", manager::getVotings);
    }

    @Override
    @GET
    @Path("/blackList")
    @Produces("application/json")
    public RequestResult<BlockedPacket> getBlackList() {
        return execute("getBlackList", manager::getBlackList);
    }
}
