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

package uk.dsxt.voting.masterclient;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.datamodel.Participant;
import uk.dsxt.voting.common.networking.*;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import java.math.BigDecimal;
import java.util.Properties;

@Log4j2
public class VotingMasterClientMain {

    public static final String MODULE_NAME = "master-client";

    private static WalletManager walletManager;
    private static MoneyDistributor distributor;

    public static void main(String[] args) {
        try {
            log.info("Starting module {}...", MODULE_NAME.toUpperCase());
            Properties properties = PropertiesHelper.loadProperties(MODULE_NAME);
            long newMessagesRequestInterval = Integer.parseInt(properties.getProperty("new_messages.request_interval", "1")) * 60000;

            String registriesServerUrl = properties.getProperty("register.server.url");
            int connectionTimeout = Integer.parseInt(properties.getProperty("http.connection.timeout"));
            int readTimeout = Integer.parseInt(properties.getProperty("http.read.timeout"));
            BigDecimal moneyToNode = new BigDecimal(properties.getProperty("money", "1"));

            final boolean useMockWallet = Boolean.valueOf(properties.getProperty("mock.wallet", Boolean.TRUE.toString()));
            walletManager = useMockWallet ? new MockWalletManager() : new BaseWalletManager(properties);

            RegistriesServer registriesServer = new RegistriesServerImpl(registriesServerUrl, connectionTimeout, readTimeout);
            init(registriesServer, walletManager, moneyToNode, newMessagesRequestInterval);
            log.info("{} module is successfully started", MODULE_NAME);
        } catch (Exception e) {
            log.error("Error occurred in module {}", MODULE_NAME, e);
        }
    }

    private static void init(RegistriesServer registriesServer, WalletManager walletManager, BigDecimal moneyToNode, long newMessagesRequestInterval) {
        Participant[] participants = registriesServer.getParticipants();
        distributor = new MoneyDistributor(walletManager, participants, moneyToNode);

        distributor.run(newMessagesRequestInterval);
    }

    public static void shutdown() throws Exception {
        if (distributor != null) {
            distributor.stop();
            distributor = null;
        }
        if (walletManager != null) {
            walletManager.stopWallet();
            walletManager = null;
        }
    }
}
