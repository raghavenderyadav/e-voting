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

package uk.dsxt.voting.resultsbuilder;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.JettyRunner;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

@Log4j2
public class ResultsBuilderMain {
    public static final String MODULE_NAME = "results-builder";

    private static org.eclipse.jetty.server.Server jettyServer;

    private static Timer timer;

    public static void main(String[] args) {
        try {
            log.info("Starting module {}...", MODULE_NAME.toUpperCase());
            Properties properties = PropertiesHelper.loadProperties(MODULE_NAME);
            ResultsManager manager = new ResultsManager();
            ResultsBuilderApplication application = new ResultsBuilderApplication(manager);

            jettyServer = JettyRunner.run(application, properties, "results.builder.web.port");

            String[] votingIds = properties.getProperty("votingIds").split(",");
            long period = (args == null ? Integer.parseInt(properties.getProperty("checkPeriod", "10")) : Integer.parseInt(args[0])) * 1000;
            timer = new Timer("ResultsPeriodicChecker timer");
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    for (String votingId : votingIds) {
                        manager.checkVoting(votingId);
                    }
                }
            }, 0L, period);
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
            timer.cancel();
        }
    }

}
