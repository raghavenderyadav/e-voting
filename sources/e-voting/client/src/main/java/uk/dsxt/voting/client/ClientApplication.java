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
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ResourceConfig;
import uk.dsxt.voting.client.auth.AuthManager;
import uk.dsxt.voting.client.datamodel.ClientsOnTime;
import uk.dsxt.voting.common.cryptoVote.CryptoVoteAcceptorWeb;
import uk.dsxt.voting.common.demo.ResultsBuilder;
import uk.dsxt.voting.common.demo.ResultsBuilderWeb;
import uk.dsxt.voting.common.demo.ResultBilderDecorator;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.domain.nodes.ClientNode;
import uk.dsxt.voting.common.domain.nodes.MasterNode;
import uk.dsxt.voting.common.domain.nodes.VotingOrganizer;
import uk.dsxt.voting.common.iso20022.Iso20022Serializer;
import uk.dsxt.voting.common.messaging.MessagesSerializer;
import uk.dsxt.voting.common.messaging.SimpleSerializer;
import uk.dsxt.voting.common.networking.MessageHandler;
import uk.dsxt.voting.common.networking.MockWalletManager;
import uk.dsxt.voting.common.networking.WalletManager;
import uk.dsxt.voting.common.networking.WalletMessageConnector;
import uk.dsxt.voting.common.nxt.NxtWalletManager;
import uk.dsxt.voting.common.registries.FileRegisterServer;
import uk.dsxt.voting.common.registries.RegistriesServer;
import uk.dsxt.voting.common.registries.RegistriesServerWeb;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.PropertiesHelper;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;
import uk.dsxt.voting.common.utils.web.JettyRunner;

import javax.ws.rs.ApplicationPath;
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
    private final NetworkScheduler networkScheduler;
    private final MessageHandler messageHandler;
    private final WalletManager walletManager;

    public ClientApplication(Properties properties, boolean isMain, String ownerId, String privateKey, String messagesFileContent, String walletOffSchedule,
                             String mainAddress, String passphrase, String nxtPropertiesPath,
                             String parentHolderUrl, String credentialsFilePath, String clientsFilePath, String stateFilePath, Logger audit) throws Exception {
        CryptoHelper cryptoHelper = CryptoHelper.DEFAULT_CRYPTO_HELPER;

        long newMessagesRequestInterval = Integer.parseInt(properties.getProperty("new_messages.request_interval", "1")) * 1000;
        String registriesServerUrl = properties.getProperty("register.server.url");
        String resultsBuilderUrl = properties.getProperty("results.builder.url");
        int connectionTimeout = Integer.parseInt(properties.getProperty("http.connection.timeout", "15000"));
        int readTimeout = Integer.parseInt(properties.getProperty("http.read.timeout", "60000"));

        final boolean useMockWallet = Boolean.valueOf(properties.getProperty("mock.wallet", Boolean.TRUE.toString()));
        walletManager = useMockWallet ? new MockWalletManager() : new NxtWalletManager(properties, nxtPropertiesPath, ownerId, mainAddress, passphrase, connectionTimeout, readTimeout);

        final boolean useMockRegistriesServer = Boolean.valueOf(properties.getProperty("mock.registries", Boolean.TRUE.toString()));
        RegistriesServer registriesServer = useMockRegistriesServer ? new FileRegisterServer(properties, null) : new RegistriesServerWeb(registriesServerUrl, connectionTimeout, readTimeout);

        Participant[] participants = registriesServer.getParticipants();
        Map<String, Participant> participantsById = Arrays.stream(participants).collect(Collectors.toMap(Participant::getId, Function.identity()));

        PrivateKey ownerPrivateKey = cryptoHelper.loadPrivateKey(privateKey);

        final boolean useSimpleSerializer = Boolean.valueOf(properties.getProperty("mock.serializer", Boolean.TRUE.toString()));
        MessagesSerializer messagesSerializer = useSimpleSerializer ? new SimpleSerializer() : new Iso20022Serializer();
        
        WalletMessageConnector walletMessageConnector = new WalletMessageConnector(walletManager, messagesSerializer, cryptoHelper, participantsById, ownerPrivateKey, ownerId, MasterNode.MASTER_HOLDER_ID);

        ClientNode clientNode;
        VotingOrganizer votingOrganizer;
        CryptoVoteAcceptorWeb acceptorWeb;
        if (isMain != MasterNode.MASTER_HOLDER_ID.equals(ownerId))
            throw new IllegalArgumentException("isMain != MasterNode.MASTER_HOLDER_ID.equals(ownerId)");
        if (isMain) {
            int calculateResultsDelay = Integer.parseInt(properties.getProperty("calculate.results.delay", "60")) * 1000;
            votingOrganizer = new VotingOrganizer(messagesSerializer, cryptoHelper, participantsById, ownerPrivateKey, calculateResultsDelay);
            walletMessageConnector.addClient(votingOrganizer);
            clientNode = new MasterNode(messagesSerializer, cryptoHelper, participantsById, ownerPrivateKey);
            acceptorWeb = null;
        } else {
            votingOrganizer = null;
            StateFileSerializer stateFileSerializer = new StateFileSerializer(stateFilePath);
            acceptorWeb = parentHolderUrl == null || parentHolderUrl.isEmpty() ? null : new CryptoVoteAcceptorWeb(parentHolderUrl, connectionTimeout, readTimeout, null);
            clientNode = new ClientNode(ownerId, messagesSerializer, cryptoHelper, participantsById, ownerPrivateKey, acceptorWeb, stateFileSerializer.load(), stateFileSerializer::save);
        }
        loadClients(clientNode, clientsFilePath);

        if (resultsBuilderUrl == null) {
            walletMessageConnector.addClient(clientNode);
        } else {
            ResultsBuilder resultsBuilder = new ResultsBuilderWeb(resultsBuilderUrl, connectionTimeout, readTimeout);
            walletMessageConnector.addClient(new ResultBilderDecorator(resultsBuilder, clientNode, ownerId));
        }

        messageHandler = new MessageHandler(walletManager, cryptoHelper, participants, walletMessageConnector::handleNewMessage);

        messageHandler.run(newMessagesRequestInterval);

        if (votingOrganizer != null) {
            String votingFiles = properties.getProperty("voting.files", "");
            MessagesSerializer isoSerializer = new Iso20022Serializer();
            final boolean adjustVotingTime = Boolean.valueOf(properties.getProperty("voting.adjust.time", Boolean.TRUE.toString()));
            for (String votingFile : votingFiles.split(",")) {
                String votingMessage = PropertiesHelper.getResourceString(votingFile, "windows-1251");
                Voting voting = isoSerializer.deserializeVoting(votingMessage);
                if (adjustVotingTime) {
                    long now = System.currentTimeMillis();
                    voting = new Voting(voting.getId(), voting.getName(), now, now + voting.getEndTimestamp() - voting.getBeginTimestamp(), voting.getQuestions(), voting.getSecurity());
                }
                boolean found = false;
                while (!found) {
                    votingOrganizer.addNewVoting(voting);
                    Thread.sleep(10000);
                    for (Voting receivedVoting : clientNode.getVotings()) {
                        if (voting.getId().equals(receivedVoting.getId())) {
                            log.debug("Voting with id {} was accepted", voting.getId());
                            found = true;
                            break;
                        }
                    }
                    log.debug("Voting with id {} was not accepted yet", voting.getId());
                }
            }
        }

        JettyRunner.configureMapper(this);
        HolderApiResource holderApiResource = new HolderApiResource(clientNode);
        this.registerInstances(new VotingApiResource(new ClientManager(clientNode, audit, participantsById), new AuthManager(credentialsFilePath, audit, participantsById)), holderApiResource);

        voteScheduler = messagesFileContent == null ? null : new VoteScheduler(clientNode, messagesFileContent, ownerId);
        networkScheduler = walletOffSchedule == null ? null : new NetworkScheduler(walletOffSchedule, walletManager, acceptorWeb, holderApiResource);
    }

    private void loadClients(ClientNode node, String clientsFilePath) {
        ClientsOnTime[] clientsOnTimes;
        try {
            clientsOnTimes = PropertiesHelper.loadResource(clientsFilePath, ClientsOnTime[].class);
        } catch (InternalLogicException e) {
            log.error("loadClients failed: {}", e.getMessage());
            return;
        }
        long now = System.currentTimeMillis();
        for (ClientsOnTime clientsOnTime : clientsOnTimes) {
            node.setClientsOnTime(now + clientsOnTime.getMinutes() * 60000, clientsOnTime.getClients());
        }
    }

    public void stop() {
        if (voteScheduler != null)
            voteScheduler.stop();
        if (networkScheduler != null)
            networkScheduler.stop();
        messageHandler.stop();
        walletManager.stop();
    }
}
