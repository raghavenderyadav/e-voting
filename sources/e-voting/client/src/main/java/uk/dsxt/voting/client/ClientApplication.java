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
import uk.dsxt.voting.common.demo.WalletMessageConnectorWithResultBuilderClient;
import uk.dsxt.voting.common.domain.dataModel.Client;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.domain.nodes.ClientNode;
import uk.dsxt.voting.common.iso20022.ISO20022Serializer;
import uk.dsxt.voting.common.iso20022.jaxb.MeetingInstruction;
import uk.dsxt.voting.common.messaging.CryptoNodeDecorator;
import uk.dsxt.voting.common.messaging.CryptoVoteAcceptorWeb;
import uk.dsxt.voting.common.messaging.MessagesSerializer;
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
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@ApplicationPath("")
public class ClientApplication extends ResourceConfig {

    private final VoteScheduler voteScheduler;
    private final WalletScheduler walletScheduler;
    private final MessageHandler messageHandler;
    private final WalletManager walletManager;

    public ClientApplication(Properties properties, String[] args) throws Exception {
        CryptoHelper cryptoHelper = CryptoHelper.DEFAULT_CRYPTO_HELPER;
        String ownerId = args == null ? properties.getProperty("owner.id") : args[3];
        PrivateKey ownerPrivateKey = cryptoHelper.loadPrivateKey(args == null ? properties.getProperty("owner.private_key") : args[4]);
        String messagesFileContent = args == null ? PropertiesHelper.getResourceString(properties.getProperty("scheduled_messages.file_path")) : args[5];
        String walletOffSchedule = args == null ? PropertiesHelper.getResourceString(properties.getProperty("walletoff_schedule.file_path")) : args[6];

        long newMessagesRequestInterval = Integer.parseInt(properties.getProperty("new_messages.request_interval", "1")) * 1000;

        String registriesServerUrl = properties.getProperty("register.server.url");
        String resultsBuilderUrl = properties.getProperty("results.builder.url");
        String parentHolderUrl = properties.getProperty("parent.holder.url");
        int connectionTimeout = Integer.parseInt(properties.getProperty("http.connection.timeout"));
        int readTimeout = Integer.parseInt(properties.getProperty("http.read.timeout"));

        final boolean useMockWallet = Boolean.valueOf(properties.getProperty("mock.wallet", Boolean.TRUE.toString()));
        walletManager = useMockWallet ? new MockWalletManager() : new NxtWalletManager(properties, args, ownerId);

        RegistriesServer registriesServer = new RegistriesServerWeb(registriesServerUrl, connectionTimeout, readTimeout);
        CryptoVoteAcceptorWeb cryptoVoteAcceptorWeb = parentHolderUrl == null || parentHolderUrl.isEmpty() ? null : new CryptoVoteAcceptorWeb(parentHolderUrl, connectionTimeout, readTimeout);
        ResultsBuilder resultsBuilder = new ResultsBuilderWeb(resultsBuilderUrl, connectionTimeout, readTimeout);

        Participant[] participants = registriesServer.getParticipants();
        Map<String, Participant> participantsById = Arrays.stream(participants).collect(Collectors.toMap(Participant::getId, Function.identity()));

        ClientNode clientNode = new ClientNode(ownerId);
        MessagesSerializer messagesSerializer = new ISO20022Serializer();
        CryptoNodeDecorator cryptoNodeDecorator = new CryptoNodeDecorator(clientNode, cryptoVoteAcceptorWeb, messagesSerializer, cryptoHelper, participantsById, ownerPrivateKey);

        WalletMessageConnectorWithResultBuilderClient walletMessageConnectorWithResultBuilderClient = new WalletMessageConnectorWithResultBuilderClient(resultsBuilder,
                walletManager, clientNode, new ISO20022Serializer(), cryptoHelper, participantsById, ownerPrivateKey, ownerId, "0");

        messageHandler = new MessageHandler(walletManager, cryptoHelper, participants, walletMessageConnectorWithResultBuilderClient::handleNewMessage);

        voteScheduler = new VoteScheduler(clientNode, messagesFileContent, ownerId);
        walletScheduler = new WalletScheduler(walletManager, walletOffSchedule);

        messageHandler.run(newMessagesRequestInterval);


        JAXBContext miContext = JAXBContext.newInstance(MeetingInstruction.class);
        Unmarshaller miUnmarshaller = miContext.createUnmarshaller();
        String miParticipantsXml = PropertiesHelper.getResourceString(properties.getProperty("participants_xml.file_path"), "windows-1251");
        StringReader miReader = new StringReader(miParticipantsXml);
        MeetingInstruction mi = (MeetingInstruction) JAXBIntrospector.getValue(miUnmarshaller.unmarshal(miReader));

        JettyRunner.configureMapper(this);
        HolderApiResource holderApiResource = new HolderApiResource(cryptoNodeDecorator);
        this.registerInstances(new VotingApiResource(new ClientManager(clientNode, mi)), holderApiResource);
    }

    public void stop() {
        voteScheduler.stop();
        walletScheduler.stop();
        messageHandler.stop();
        walletManager.stopWallet();
    }
}
