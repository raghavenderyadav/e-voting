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
import uk.dsxt.voting.common.datamodel.BlockedPacket;
import uk.dsxt.voting.common.datamodel.Participant;
import uk.dsxt.voting.common.datamodel.Voting;
import uk.dsxt.voting.common.datamodel.Holding;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Log4j2
@Path("/voting-api")
public class RegistriesServerResource implements uk.dsxt.voting.common.networking.RegistriesServer {
    private final RegistriesServerManager manager;

    public RegistriesServerResource(RegistriesServerManager manager) {
        this.manager = manager;
    }

    @Override
    @GET
    @Path("/holdings")
    @Produces("application/json")
    public Holding[] getHoldings() {
        return manager.getHoldings();
    }

    @Override
    @GET
    @Path("/participants")
    @Produces("application/json")
    public Participant[] getParticipants() {
        return manager.getParticipants();
    }

    @Override
    @GET
    @Path("/votings")
    @Produces("application/json")
    public Voting[] getVotings() {
        return manager.getVotings();
    }

    @Override
    @GET
    @Path("/blackList")
    @Produces("application/json")
    public BlockedPacket[] getBlackList() {
        return manager.getBlackList();
    }
}
