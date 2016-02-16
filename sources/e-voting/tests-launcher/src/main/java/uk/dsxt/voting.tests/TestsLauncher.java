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

package uk.dsxt.voting.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.joda.time.Instant;
import uk.dsxt.voting.client.VotingClientMain;
import uk.dsxt.voting.common.datamodel.Voting;
import uk.dsxt.voting.common.networking.RegistriesServer;
import uk.dsxt.voting.common.networking.RegistriesServerImpl;
import uk.dsxt.voting.common.utils.PropertiesHelper;
import uk.dsxt.voting.masterclient.VotingMasterClientMain;
import uk.dsxt.voting.registriesserver.RegistriesServerMain;
import uk.dsxt.voting.resultsbuilder.ResultsBuilderMain;

import java.io.FileOutputStream;
import java.util.Properties;

@Log4j2
public class TestsLauncher {
    public static final String MODULE_NAME = "tests-launcher";

    private static final String MASTER_NAME = "nxt";
    private static final String DEFAULT_TESTNET_PEERS = "127.0.0.1:7873";

    @FunctionalInterface
    public interface SimpleRequest {
        void run();
    }

    public static void main(String[] args) {
        try {
            log.debug("Starting module {}...", MODULE_NAME);
            ObjectMapper mapper = new ObjectMapper();
            //read configuration
            Properties properties = PropertiesHelper.loadProperties(MODULE_NAME);
            int votingDuration = Integer.valueOf(properties.getProperty("voting.duration.minutes"));
            String testingType = properties.getProperty("testing.type");
            log.info("Testing type is {}", testingType);
            String registriesServerUrl = properties.getProperty("register.server.url");
            int connectionTimeout = Integer.parseInt(properties.getProperty("http.connection.timeout"));
            int readTimeout = Integer.parseInt(properties.getProperty("http.read.timeout"));
            //json file configuration for clients
            String configFileName = properties.getProperty("client.config.file");
            String resourceJson = PropertiesHelper.getResourceString(String.format(configFileName, testingType));
            ClientConfiguration[] configurations = mapper.readValue(resourceJson, ClientConfiguration[].class);

            //starting single modules
            startSingleModule(RegistriesServerMain.MODULE_NAME, () -> RegistriesServerMain.main(new String[]{testingType, String.valueOf(votingDuration)}));
            startSingleModule(ResultsBuilderMain.MODULE_NAME, () -> ResultsBuilderMain.main(null));
            //load properties and set master node to offline mode
            final String propertiesPath = String.format("conf/%s.properties", MASTER_NAME);

            Properties nxtProperties = PropertiesHelper.loadProperties("nxt-default");
            nxtProperties.setProperty("nxt.peerServerPort", "7873");
            nxtProperties.setProperty("nxt.apiServerPort", "7872");
            nxtProperties.setProperty("nxt.testDbDir", "./nxt");
            nxtProperties.setProperty("nxt.defaultTestnetPeers", DEFAULT_TESTNET_PEERS);
            nxtProperties.setProperty("nxt.isOffline", "true");
            nxtProperties.setProperty("nxt.isTestnet", "false");
            saveProperties(propertiesPath, nxtProperties);
            startSingleModule(VotingMasterClientMain.MODULE_NAME, () -> VotingMasterClientMain.main(new String[]{propertiesPath}));
            //starting clients
            long start = Instant.now().getMillis();
            log.debug("Starting {} instances of {}", configurations.length, VotingClientMain.MODULE_NAME);
            int startPort = 9000;
            final String[] propertiesPathArray = new String[1];
            for (int i = 0; i < configurations.length; i++) {
                ClientConfiguration conf = configurations[i];
                String clientName = String.format("nxt-node-%s", conf.getHolderId());
                propertiesPathArray[0] = String.format("conf/%s.properties", clientName);
                String dbDir = String.format("./%s", clientName);
                nxtProperties.setProperty("nxt.apiServerPort", String.valueOf(startPort + 2 * i));
                nxtProperties.setProperty("nxt.peerServerPort", String.valueOf(startPort + 2 * i + 1));
                nxtProperties.setProperty("nxt.defaultTestnetPeers", DEFAULT_TESTNET_PEERS);
                nxtProperties.setProperty("nxt.isOffline", "false");
                nxtProperties.setProperty("nxt.isTestnet", "true");
                nxtProperties.setProperty("nxt.testDbDir", dbDir);
                saveProperties(propertiesPathArray[0], nxtProperties);
                VotingClientMain.main(new String[]{conf.getHolderId(), conf.getPrivateKey(), conf.getVote(), propertiesPathArray[0]});
            }
            log.info("{} instances of {} started in {} ms", configurations.length, RegistriesServerMain.MODULE_NAME, Instant.now().getMillis() - start);
            //need to wait until voting is complete
            RegistriesServer regServer = new RegistriesServerImpl(registriesServerUrl, connectionTimeout, readTimeout);
            Voting[] votings = regServer.getVotings();
            if (votings.length > 1) {
                log.error("There is more than one voting. Stopping {}", MODULE_NAME);
                return;
            }
            Voting currentVoting = regServer.getVotings()[0];
            long sleepPeriod = currentVoting.getEndTimestamp() - Instant.now().getMillis();
            log.info("Waiting {} seconds while voting ends", sleepPeriod / 1000);
            Thread.sleep(sleepPeriod);

            //TODO: check that results builder has finished calculating results

            //stop jetty servers
            RegistriesServerMain.shutdown();
            ResultsBuilderMain.shutdown();
            //stop other modules
            VotingMasterClientMain.shutdown();
            //TODO: stop clients
            log.info("Testing finished");
        } catch (Exception e) {
            log.error("Error occurred in module {}", MODULE_NAME, e);
        }
    }

    private static void startSingleModule(String name, SimpleRequest request) {
        log.debug("Starting {}", name);
        long start = Instant.now().getMillis();
        request.run();
        log.info("{} started in {} ms", name, Instant.now().getMillis() - start);
    }

    private static void saveProperties(String path, Properties properties) {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            properties.store(fos, "");
        } catch (Exception e) {
            String errorMessage = String.format("Can't save property. Error: %s", e.getMessage());
            log.error(errorMessage, e);
            throw new RuntimeException(errorMessage);
        }
    }
}
