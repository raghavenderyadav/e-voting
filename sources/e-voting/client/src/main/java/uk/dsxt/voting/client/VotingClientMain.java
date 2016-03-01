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
import uk.dsxt.voting.common.datamodel.*;
import uk.dsxt.voting.common.networking.*;
import uk.dsxt.voting.common.utils.CryptoHelper;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import java.security.PrivateKey;
import java.util.Properties;

@Log4j2
public class VotingClientMain {

    public static final String MODULE_NAME = "client";

    public static void main(String[] args) {
        try {
            log.info("Starting module {}...", MODULE_NAME.toUpperCase());
            Properties properties = PropertiesHelper.loadProperties(MODULE_NAME);

            if (args != null && args.length < 8)
                throw new InternalLogicException("Wrong arguments");
            String ownerId = args == null ? properties.getProperty("owner.id") : args[3];
            PrivateKey ownerPrivateKey = CryptoHelper.loadPrivateKey(args == null ? properties.getProperty("owner.private_key") : args[4]);
            String messagesFileContent = args == null ? PropertiesHelper.getResourceString(properties.getProperty("scheduled_messages.file_path")) : args[5];
            String walletOffSchedule = args == null ? PropertiesHelper.getResourceString(properties.getProperty("walletoff_schedule.file_path")) : args[6];

            long newMessagesRequestInterval = Integer.parseInt(properties.getProperty("new_messages.request_interval", "1")) * 1000;

            String registriesServerUrl = properties.getProperty("register.server.url");
            String resultsBuilderUrl = properties.getProperty("results.builder.url");
            int connectionTimeout = Integer.parseInt(properties.getProperty("http.connection.timeout"));
            int readTimeout = Integer.parseInt(properties.getProperty("http.read.timeout"));

            final boolean useMockWallet = Boolean.valueOf(properties.getProperty("mock.wallet", Boolean.TRUE.toString()));
            WalletManager walletManager = useMockWallet ? new MockWalletManager() : new BaseWalletManager(properties, args, ownerId);

            RegistriesServer registriesServer = new RegistriesServerImpl(registriesServerUrl, connectionTimeout, readTimeout);
            ResultsBuilder resultsBuilder = new ResultsBuilderImpl(resultsBuilderUrl, connectionTimeout, readTimeout);
            init(registriesServer, walletManager, resultsBuilder, ownerId, ownerPrivateKey, messagesFileContent, newMessagesRequestInterval, walletOffSchedule);
            log.info("{} module is successfully started", MODULE_NAME);
        } catch (Exception e) {
            log.error("Error occurred in module {}", MODULE_NAME, e);
        }
    }

    private static void init(RegistriesServer registriesServer, WalletManager walletManager, ResultsBuilder resultsBuilder, String ownerId, PrivateKey ownerPrivateKey,
                             String messagesFileContent, long newMessagesRequestInterval, String walletOffSchedule) {
        Participant[] participants = registriesServer.getParticipants();
        Client[] clients = null; //TODO load clients from resources or database;
        Voting[] votings = null; //TODO get all votings;

        VoteAggregation aggregation = new VoteAggregation(clients);
        VotingClient client = new VotingClient(walletManager, aggregation, resultsBuilder, ownerId, ownerPrivateKey, participants);
        VoteScheduler voteScheduler = new VoteScheduler(client, resultsBuilder, aggregation, votings, messagesFileContent, ownerId);
        WalletScheduler walletScheduler = new WalletScheduler(walletManager);

        client.run(newMessagesRequestInterval);
        voteScheduler.run();
        walletScheduler.run(walletOffSchedule);
    }

}
