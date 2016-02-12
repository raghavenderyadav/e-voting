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

package uk.dsxt.voting.masterclient;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.datamodel.Participant;
import uk.dsxt.voting.common.networking.RegistriesServer;
import uk.dsxt.voting.common.networking.WalletManager;
import uk.dsxt.voting.common.utils.PropertiesHelper;
import uk.dsxt.voting.common.networking.BaseWalletManager;

import java.math.BigDecimal;
import java.util.Properties;

@Log4j2
public class VotingMasterClientMain {

    private static final String MODULE_NAME = "master-client";

    public static void main(String[] args) {
        try {
            log.info(String.format("Starting module %s...", MODULE_NAME.toUpperCase()));
            Properties properties = PropertiesHelper.loadProperties(MODULE_NAME);
            long newMessagesRequestInterval = Integer.parseInt(properties.getProperty("new_messages.request_interval", "1")) * 60000;
            BigDecimal moneyToNode = new BigDecimal(properties.getProperty("money", "1"));
            WalletManager walletManager = new BaseWalletManager(properties);
            walletManager.runWallet();
            RegistriesServer registriesServer = null; //TODO
            init(registriesServer, walletManager, moneyToNode, newMessagesRequestInterval);
            log.info(String.format("%s module is successfully started", MODULE_NAME));
        } catch (Exception e) {
            log.error(String.format("Error occurred in module %s", MODULE_NAME), e);
        }
    }

    private static void init(RegistriesServer registriesServer, WalletManager walletManager, BigDecimal moneyToNode, long newMessagesRequestInterval) {
        Participant[] participants = registriesServer.getParticipants();
        MoneyDistributer distributer = new MoneyDistributer(walletManager, participants, moneyToNode);

        distributer.run(newMessagesRequestInterval);
    }

}
