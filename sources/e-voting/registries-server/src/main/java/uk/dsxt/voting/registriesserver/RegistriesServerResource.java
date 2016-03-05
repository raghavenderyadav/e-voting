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
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.registries.RegistriesServer;
import uk.dsxt.voting.common.registries.SimpleRegisterServer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.function.Supplier;

@Log4j2
@Path("/voting-api")
public class RegistriesServerResource implements RegistriesServer {
    private final RegistriesServer server;

    public RegistriesServerResource(RegistriesServer server) {
        this.server = server;
    }

    private <T> T execute(String name, Supplier<T> request) {
        try {
            return request.get();
        } catch (Exception ex) {
            log.error("{} failed", name, ex);
            try {
                log.warn("stop called.");
                RegistriesServerMain.shutdown();
            } catch (Exception e) {
                log.error("stop failed. unable to stop module.", e);
            }
            return null;
        }
    }

    @Override
    @GET
    @Path("/participants")
    @Produces("application/json")
    public Participant[] getParticipants() {
        return execute("getParticipants", server::getParticipants);
    }
}
