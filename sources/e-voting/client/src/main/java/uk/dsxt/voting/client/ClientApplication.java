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
import org.glassfish.jersey.server.ResourceConfig;
import uk.dsxt.voting.common.demo.ResultsBuilder;
import uk.dsxt.voting.common.demo.ResultsBuilderWeb;
import uk.dsxt.voting.common.domain.dataModel.Client;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.iso20022.jaxb.MeetingInstruction;
import uk.dsxt.voting.common.messaging.WalletManager;
import uk.dsxt.voting.common.networking.*;
import uk.dsxt.voting.common.nxt.NxtWalletManager;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;
import uk.dsxt.voting.common.utils.web.JettyRunner;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import javax.ws.rs.ApplicationPath;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.security.PrivateKey;
import java.util.Properties;

@Log4j2
@ApplicationPath("")
public class ClientApplication extends ResourceConfig {
    private static final CryptoHelper cryptoHelper = CryptoHelper.DEFAULT_CRYPTO_HELPER;


    public ClientApplication(Properties properties, String[] args) throws Exception {
        String ownerId = args == null ? properties.getProperty("owner.id") : args[3];
        PrivateKey ownerPrivateKey = cryptoHelper.loadPrivateKey(args == null ? properties.getProperty("owner.private_key") : args[4]);
        String messagesFileContent = args == null ? PropertiesHelper.getResourceString(properties.getProperty("scheduled_messages.file_path")) : args[5];
        String walletOffSchedule = args == null ? PropertiesHelper.getResourceString(properties.getProperty("walletoff_schedule.file_path")) : args[6];

        long newMessagesRequestInterval = Integer.parseInt(properties.getProperty("new_messages.request_interval", "1")) * 1000;

        String registriesServerUrl = properties.getProperty("register.server.url");
        String resultsBuilderUrl = properties.getProperty("results.builder.url");
        int connectionTimeout = Integer.parseInt(properties.getProperty("http.connection.timeout"));
        int readTimeout = Integer.parseInt(properties.getProperty("http.read.timeout"));

        final boolean useMockWallet = Boolean.valueOf(properties.getProperty("mock.wallet", Boolean.TRUE.toString()));
        WalletManager walletManager = useMockWallet ? new MockWalletManager() : new NxtWalletManager(properties, args, ownerId);

        RegistriesServer registriesServer = new RegistriesServerWeb(registriesServerUrl, connectionTimeout, readTimeout);
        ResultsBuilder resultsBuilder = new ResultsBuilderWeb(resultsBuilderUrl, connectionTimeout, readTimeout);
        init(registriesServer, walletManager, resultsBuilder, ownerId, ownerPrivateKey, messagesFileContent, newMessagesRequestInterval, walletOffSchedule);

        JAXBContext miContext = JAXBContext.newInstance(MeetingInstruction.class);
        Unmarshaller miUnmarshaller = miContext.createUnmarshaller();
        String miParticipantsXml = PropertiesHelper.getResourceString(properties.getProperty("participants_xml.file_path"), "windows-1251");
        StringReader miReader = new StringReader(miParticipantsXml);
        MeetingInstruction mi = (MeetingInstruction) JAXBIntrospector.getValue(miUnmarshaller.unmarshal(miReader));

        JettyRunner.configureMapper(this);
        this.registerInstances(new VotingApiResource(new ClientManager(mi)));
    }

    private static void init(RegistriesServer registriesServer, WalletManager walletManager, ResultsBuilder resultsBuilder, String ownerId, PrivateKey ownerPrivateKey,
                             String messagesFileContent, long newMessagesRequestInterval, String walletOffSchedule) {
        Participant[] participants = new Participant[0];
        Client[] clients = null; //TODO load clients from resources or database;
        Voting[] votings = new Voting[0]; //TODO get all votings;

        VoteAggregation aggregation = new VoteAggregation(clients);
        VotingClient client = new VotingClient(walletManager, aggregation, resultsBuilder, ownerId, ownerPrivateKey, participants);
        VoteScheduler voteScheduler = new VoteScheduler(client, resultsBuilder, aggregation, votings, messagesFileContent, ownerId);
        WalletScheduler walletScheduler = new WalletScheduler(walletManager);

        client.run(newMessagesRequestInterval);
        voteScheduler.run();
        walletScheduler.run(walletOffSchedule);
    }
}
