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
import uk.dsxt.voting.common.datamodel.InternalLogicException;
import uk.dsxt.voting.common.utils.JettyRunner;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import java.util.Properties;

@Log4j2
public class RegistriesServerMain {
    private static final String MODULE_NAME = "registries-server";

    private static org.eclipse.jetty.server.Server jettyServer;

    public static void main(String[] args) {
        try {
            log.info("Starting module {}...", MODULE_NAME.toUpperCase());
            Properties properties = PropertiesHelper.loadProperties(MODULE_NAME);
            RegistriesServerApplication application = new RegistriesServerApplication(properties);
            jettyServer = JettyRunner.run(application, properties, "registries.server.web.port");
            log.info("{} module is successfully started", MODULE_NAME);
        } catch (InternalLogicException e) {
            log.error("Logic exception in module {}. Reason: {}", MODULE_NAME, e.getMessage());
        } catch (Exception e) {
            log.error("Error occurred in module {}", MODULE_NAME, e);
        }
    }

    public static void shutdown() throws Exception {
        if (jettyServer != null) {
            jettyServer.stop();
            jettyServer.destroy();
            jettyServer = null;
        }
    }

}
