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

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.Instant;
import uk.dsxt.voting.client.VotingClientMain;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.registries.RegistriesServer;
import uk.dsxt.voting.common.registries.RegistriesServerWeb;
import uk.dsxt.voting.common.utils.PropertiesHelper;
import uk.dsxt.voting.registriesserver.RegistriesServerMain;
import uk.dsxt.voting.resultsbuilder.ResultsBuilderMain;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Log4j2
public class TestsLauncher implements BaseTestsLauncher {

    private static Map<String, Process> processesByName = new HashMap<>();

    private static boolean startClientsAsProcesses = true;

    private static String masterAccount;
    private static String masterPassword;

    @FunctionalInterface
    public interface SimpleRequest {
        void run();
    }

    public static void main(String[] args) {
        try {
            log.debug("Starting module {}...", MODULE_NAME);
            //delete old files
            deleteNxtFiles();
            //read configuration
            Properties properties = PropertiesHelper.loadProperties(MODULE_NAME);
            int votingDuration = Integer.valueOf(properties.getProperty("voting.duration.minutes"));
            String testingType = properties.getProperty("testing.type");
            log.info("Testing type is {}", testingType);
            String registriesServerUrl = properties.getProperty("register.server.url");
            int connectionTimeout = Integer.parseInt(properties.getProperty("http.connection.timeout"));
            int readTimeout = Integer.parseInt(properties.getProperty("http.read.timeout"));
            int resultsCheckPeriod = Integer.parseInt(properties.getProperty("results.check.period"));
            int clientAggregationPeriod = Integer.parseInt(properties.getProperty("client.results.aggregation.period"));
            String allowedHosts = properties.getProperty("allowed.hosts");

            masterAccount = properties.getProperty("master.address");
            masterPassword = properties.getProperty("master.passphrase");

            NXTAccount[] nxtAccounts = PropertiesHelper.loadResource("json/nxtAccounts.json", NXTAccount[].class);
            log.info("Found {} accounts.", nxtAccounts.length);

            //json file configuration for clients
            ClientConfiguration[] configurations = PropertiesHelper.loadResource(properties, null, "client.config.file", ClientConfiguration[].class);
            startClientsAsProcesses = Boolean.parseBoolean(properties.getProperty("testing.clients_as_processes"));

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < configurations.length; i++) {
                if (configurations[i].isVictim()) ;
                builder.append(nxtAccounts[i].getAccount());
                builder.append(";");
            }
            String victimAccounts = builder.toString();
            if (victimAccounts.length() > 0)
                victimAccounts = victimAccounts.substring(0, victimAccounts.length() - 1);

            //starting single modules
            startSingleModule(RegistriesServerMain.MODULE_NAME, () -> RegistriesServerMain.main(new String[]{testingType, String.valueOf(votingDuration)}));
            startSingleModule(ResultsBuilderMain.MODULE_NAME, () -> ResultsBuilderMain.main(new String[]{String.valueOf(resultsCheckPeriod)}));
            //load properties and set master node to offline mode

            Properties nxtProperties = PropertiesHelper.loadProperties("nxt-default");
            nxtProperties.setProperty("nxt.enablePeerServerDoSFilter", "true");
            nxtProperties.setProperty("nxt.peerServerDoSFilter.maxRequestMs", "300000");
            nxtProperties.setProperty("nxt.peerServerDoSFilter.delayMs", "1000");
            nxtProperties.setProperty("nxt.peerServerDoSFilter.maxRequestsPerSec", "3000");
            nxtProperties.setProperty("nxt.sendToPeersLimit", "1000");
            nxtProperties.setProperty("nxt.testUnconfirmedTransactions", "true");
            nxtProperties.setProperty("nxt.pushThreshold", "0");

            nxtProperties.setProperty("nxt.evt.sendNxtBlackList", String.format("%s", masterAccount));

            final String propertiesPath = createWalletPropertiesFile(MASTER_NAME, 7872, nxtProperties, allowedHosts);
            //starting clients
            long start = Instant.now().getMillis();
            log.debug("Starting {} instances of {}", configurations.length, VotingClientMain.MODULE_NAME);
            int startPort = 9000;
            for (int i = 0; i < configurations.length; i++) {
                final int ii = i;
                ClientConfiguration conf = configurations[i];
                String clientName = String.format("nxt-node-%s", conf.getHolderId());
                String blackList = !conf.isHonestParticipant() ? victimAccounts : "";
                nxtProperties.setProperty("nxt.evt.blackList", blackList);
                String clientPropertiesPath = createWalletPropertiesFile(clientName, startPort + 2 * i, nxtProperties, allowedHosts);
                String walletOffSchedule = conf.getDisconnectMask() == null ? ";" : conf.getDisconnectMask();
                startClient(ii, configurations, clientPropertiesPath, walletOffSchedule, clientAggregationPeriod, nxtAccounts);
            }
            log.info("{} instances of {} started in {} ms", configurations.length, RegistriesServerMain.MODULE_NAME, Instant.now().getMillis() - start);
            //need to wait until voting is complete
            RegistriesServer regServer = new RegistriesServerWeb(registriesServerUrl, connectionTimeout, readTimeout);
            /*
            Voting[] votings = regServer.getVotings();
            if (votings.length > 1) {
                log.error("There is more than one voting. Stopping {}", MODULE_NAME);
                return;
            }
            Voting currentVoting = regServer.getVotings()[0];
            long sleepPeriod = currentVoting.getEndTimestamp() - Instant.now().getMillis();
            log.info("Waiting {} seconds while voting ends", sleepPeriod / 1000);
            Thread.sleep(sleepPeriod);
            */
            log.info("----------------------------- TEST FINISHED -----------------------------");
            // TODO: check that results builder has finished calculating results
            Thread.sleep(60 * 60 * 1000);
            stopAllProcesses();
            //stop jetty servers
            RegistriesServerMain.shutdown();
            ResultsBuilderMain.shutdown();
            log.info("Testing finished");
        } catch (Exception e) {
            log.error("Error occurred in module {}", MODULE_NAME, e);
        }
    }

    private static String createWalletPropertiesFile(String nodeName, int port, Properties nxtProperties, String allowedHosts) {
        String clientPropertiesPath = String.format("conf/%s.properties", nodeName);
        final Path path = Paths.get(".", DB_FOLDER, nodeName);
        nxtProperties.setProperty("nxt.apiServerPort", String.valueOf(port));
        nxtProperties.setProperty("nxt.peerServerPort", String.valueOf(port + 1));
        nxtProperties.setProperty("nxt.defaultTestnetPeers", DEFAULT_TESTNET_PEERS);
        nxtProperties.setProperty("nxt.isOffline", "false");
        nxtProperties.setProperty("nxt.isTestnet", "true");
        nxtProperties.setProperty("nxt.testDbDir", path.toString());
        nxtProperties.setProperty("nxt.minNeedBlocks", "1");
        nxtProperties.setProperty("nxt.testnetGuaranteedBalanceConfirmations", "1");
        nxtProperties.setProperty("nxt.allowedUserHosts", allowedHosts);
        nxtProperties.setProperty("nxt.allowedBotHosts", allowedHosts);
        nxtProperties.setProperty("nxt.apiServerHost", "0.0.0.0");
        nxtProperties.setProperty("nxt.forceScan", "true");
        //nxtProperties.setProperty("nxt.disableAdminPassword", "true");
        //nxtProperties.setProperty("nxt.forgingSpeedup", "10");
        saveProperties(clientPropertiesPath, nxtProperties);
        return clientPropertiesPath;
    }

    private static void startClient(int idx, ClientConfiguration[] configurations, String clientPropertiesPath,
                                    String walletOffSchedule, int clientAggregationPeriod, NXTAccount[] nxtAccounts) {
        ClientConfiguration conf = configurations[idx];
        final String password = nxtAccounts[idx].getPassword();
        if (startClientsAsProcesses) {
            startProcess("Client" + idx, CLIENT_JAR_PATH, new String[]{clientPropertiesPath, masterAccount, password, conf.getHolderId(), conf.getPrivateKey(),
                    conf.getVote() == null || conf.getVote().isEmpty() ? "#" : conf.getVote(), walletOffSchedule, String.valueOf(clientAggregationPeriod), idx == 0 ? "true" : "false"});
        } else {
            VotingClientMain.main(new String[]{clientPropertiesPath, masterAccount, password, conf.getHolderId(), conf.getPrivateKey(), conf.getVote(),
                    walletOffSchedule, String.valueOf(clientAggregationPeriod)});
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

    private static void deleteNxtFiles() throws Exception {
        File file = new File(System.getProperty("user.dir"));
        if (file.listFiles() == null) {
            throw new InternalLogicException(String.format("Couldn't delete files - the path is incorrect: %s", file.getAbsolutePath()));
        }
        for (File c : file.listFiles()) {
            if (c.getName().contains("nxt")) {
                log.info("Deleting {}...", c.getName());
                if (c.isDirectory()) {
                    FileUtils.deleteDirectory(c);
                } else {
                    c.delete();
                }
            }
        }
    }

    private static void startProcess(String name, String jarPath, String[] params) {
        if (processesByName.containsKey(name)) {
            stopProcess(name);
        }
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add("java");
            cmd.add("-jar");
            cmd.add(jarPath);
            Collections.addAll(cmd, params);

            log.debug("Start process command: {}", StringUtils.join(cmd, " "));

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(cmd);
            processBuilder.redirectError(new File(LOGS_FOLDER, String.format("error_%s.log", name)));
            processBuilder.redirectOutput(new File(LOGS_FOLDER, String.format("output_%s.log", name)));
            Process process = processBuilder.start();
            processesByName.put(name, process);
            log.info("Process {} started", name);
        } catch (Exception e) {
            log.error("Can't run process {}. Error: {}", e.getMessage(), name, e);
        }
    }

    private static void stopAllProcesses() {
        for (Map.Entry<String, Process> processEntry : processesByName.entrySet()) {
            try {
                processEntry.getValue().destroy();
                log.info("Process {} killed", processEntry.getKey());
            } catch (Exception e) {
                log.error("Can't kill process {}. Error: {}", e.getMessage(), processEntry.getKey(), e);
            }
        }
    }

    private static void stopProcess(String name) {
        Process process = processesByName.get(name);
        if (process == null) {
            return;
        }
        try {
            process.destroy();
            log.info("Process {} killed", name);
        } catch (Exception e) {
            log.error("Can't kill process {}. Error: {}", e.getMessage(), name, e);
        }
        processesByName.remove(name);
    }

    private static NXTAccount[] loadNxtAccounts(String content) {
        List<NXTAccount> accs = new ArrayList<>();
        String[] lines = content.split(System.lineSeparator());
        for (String line : lines) {
            String[] account = line.split(":");
            accs.add(new NXTAccount(account[0], account[1]));
        }
        return accs.toArray(new NXTAccount[accs.size()]);
    }
}
