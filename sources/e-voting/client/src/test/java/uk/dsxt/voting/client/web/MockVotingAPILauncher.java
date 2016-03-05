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

package uk.dsxt.voting.client.web;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.utils.web.JettyRunner;

import java.util.Properties;

@Log4j2
public class MockVotingAPILauncher {

    public static void main(String[] args) {
        try {
            log.info("Starting MockVotingAPILauncher...");
            Properties properties = new Properties();
            properties.put("port", "9000");
            properties.put("jetty.maxThreads", "5000");
            properties.put("jetty.minThreads", "2000");
            properties.put("jetty.maxQueueSize", "10000");
            properties.put("jetty.idleTimeout", "100000");
            MockVotingAPIApplication application = new MockVotingAPIApplication();
            JettyRunner.run(application, properties, "port");
            log.info("MockVotingAPILauncher is successfully started");
        } catch (Exception e) {
            log.error("MockVotingAPILauncher failed.", e);
        }
    }
}
