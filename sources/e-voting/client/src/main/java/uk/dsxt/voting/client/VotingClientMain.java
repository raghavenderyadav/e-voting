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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.dsxt.voting.common.utils.PropertiesHelper;
import uk.dsxt.voting.common.utils.web.JettyRunner;

import java.util.Properties;

@Log4j2
public class VotingClientMain {

    public static final String MODULE_NAME = "client";
    
    private static org.eclipse.jetty.server.Server jettyServer;
    private static ClientApplication application;

    private final static String AUDIT_LOGGER_NAME = "AUDIT";
    private static final Logger audit = LogManager.getLogger(AUDIT_LOGGER_NAME);
    
    public static void main(String[] args) {
        try {                       
            log.info("Starting module {}...", MODULE_NAME.toUpperCase());            
            audit.info("Starting module {}...", MODULE_NAME.toUpperCase());
            
            Properties properties = PropertiesHelper.loadProperties(MODULE_NAME);
            args = args == null || args.length == 0 ? null : args;
            String blockchain = args == null ? "mock" : args[0];
            String nxtPropertiesPath = args == null ? properties.getProperty("nxt.properties.path") : args[1];
            String mainAddress = args == null ? properties.getProperty("nxt.main.address") : args[2];
            String passphrase = args == null ? properties.getProperty("nxt.account.passphrase") : args[3];
            String ownerId = args == null ? properties.getProperty("owner.id") : args[4];
            String privateKey = args == null ? properties.getProperty("owner.private_key") : args[5];
            String messagesFileContent = args == null ? PropertiesHelper.getResourceString(properties.getProperty("scheduled_messages.file_path")) : args[6];
            String walletOffSchedule = args == null ? PropertiesHelper.getResourceString(properties.getProperty("walletoff_schedule.file_path")) : args[7];
            int jettyPort = Integer.valueOf(args == null ? properties.getProperty("client.web.port") : args[8]);
            boolean isMain = Boolean.valueOf(args == null ? properties.getProperty("client.isMain", "false") : args[9]);
            boolean copyWebDir = Boolean.valueOf(args == null ? properties.getProperty("client.web.copyWebDir", "true") : args[10]);
            String webDir = args == null ? properties.getProperty("client.web.webDir", "./gui-public/app") : args[11];
            String parentHolderUrl = args == null ? properties.getProperty("parent.holder.url") : args[12];
            String credentialsFilePath = args == null ? properties.getProperty("credentials.filepath") : args[13];
            String clientsFilePath = args == null ? properties.getProperty("clients.filepath") : args[14];
            String stateFilePath = args == null ? properties.getProperty("state.file_path") : args[15];
            String chainName = args == null ? properties.getProperty("fabric.chainName") : args[16];
            String admin = args == null ? properties.getProperty("fabric.admin") : args[17];
            String passphraseFabric = args == null ? properties.getProperty("fabric.passphrase") : args[18];
            String memberServiceUrl = args == null ? properties.getProperty("fabric.memberServiceUrl") : args[19];
            String keyValStore = args == null ? properties.getProperty("fabric.keyValStore") : args[20];
            String peer = args == null ? properties.getProperty("fabric.peer") : args[21];
            boolean isInit = args == null ? Boolean.valueOf(properties.getProperty("fabric.isInit")) : Boolean.valueOf(args[22]);
            int validatingPeerID = args == null ? Integer.valueOf(properties.getProperty("fabric.validatingPeerID")) : Integer.valueOf(args[23]);
            String peerToConnect = args == null ? properties.getProperty("fabric.peerToConnect") : args[24];

            log.info(String.format("We set up blockchain: %s", blockchain));
            application = new ClientApplication(blockchain, properties, isMain, ownerId, privateKey, messagesFileContent,
                walletOffSchedule, mainAddress, passphrase, nxtPropertiesPath, parentHolderUrl, credentialsFilePath,
                clientsFilePath, stateFilePath, audit, chainName, admin, passphraseFabric, memberServiceUrl,
                keyValStore, peer, isInit, validatingPeerID, peerToConnect);
            jettyServer = JettyRunner.run(application, properties, jettyPort, webDir, "/{1}(api|holderAPI){1}/{1}.*", copyWebDir);
            log.info("{} module is successfully started", MODULE_NAME);
        } catch (Exception e) {
            log.error(String.format("Error occurred in module %s", MODULE_NAME), e);
        }
    }

    public static void shutdown() throws Exception {
        if (jettyServer != null) {
            jettyServer.stop();
            jettyServer.destroy();
            jettyServer = null;
        }
        if (application != null) {
            application.stop();
            application = null;
        }
    }
}
