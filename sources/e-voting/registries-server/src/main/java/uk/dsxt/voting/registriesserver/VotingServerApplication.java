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

package uk.dsxt.voting.registriesserver;

import lombok.extern.log4j.Log4j2;
import org.glassfish.jersey.server.ResourceConfig;
import uk.dsxt.voting.common.utils.JettyRunner;

import javax.ws.rs.ApplicationPath;
import java.util.Properties;

@Log4j2
@ApplicationPath("")
public class VotingServerApplication extends ResourceConfig {
    public static final String MODULE_NAME = "voting-server";

    public VotingServerApplication(Properties properties) {
        try {
            log.info(String.format("Starting module %s...", MODULE_NAME.toUpperCase()));

            VotingServerManager manager = new VotingServerManager();

            JettyRunner.configureMapper(this);
            this.registerInstances(new VotingServerResource(manager));

            log.info(String.format("%s module is successfully started", MODULE_NAME));
        } catch (Exception e) {
            log.error(String.format("Error occurred in module %s", MODULE_NAME), e);
        }
    }



}
