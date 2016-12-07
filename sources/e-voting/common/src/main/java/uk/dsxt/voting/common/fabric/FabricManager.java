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

package uk.dsxt.voting.common.fabric;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.ChainCodeException;
import org.hyperledger.fabric.sdk.exception.EnrollmentException;
import org.hyperledger.fabric.sdk.exception.RegistrationException;

import uk.dsxt.voting.common.messaging.Message;
import uk.dsxt.voting.common.networking.WalletManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.util.*;

public class FabricManager implements WalletManager {

    private String chainName;
    private String admin;
    private String passphrase;
    private String peer;
    private String memberServiceUrl;
    private String keyValStore;
    private String peerToConnect;
    private boolean isInit;
    private int validatingPeerID;

    private ChainCodeResponse deployResponse;
    
    private Process fabricProcess;
    private Process memberService;
    private Chain chain;

    enum ChaincodeFunction {INIT, READ, WRITE}

    private static final Logger log =  LogManager.getLogger(FabricManager.class.getName());
    private static final String HOME_PATH = System.getProperty("user.home");
    
    private static final String CHAINCODE_PATH = "github.com/hyperledger/fabric/examples/chaincode/go/evoting";
    private static final String CHAINCODE_NAME = "mycc";
    private static final String AFFILIATION = "bank_a";

    private static final String DOCKER_VOLUME_SOCK = "-v /var/run/docker.sock:/var/run/docker.sock";
    private static final String DOCKER_RUN_COMMAND = "docker run --rm -i";
    private static final String DOCKER_RUN_FABRIC_MEMBERSRVC = "hyperledger/fabric-membersrvc membersrvc";
    private static final String DOCKER_PORT_MEMBERSRVC = "-p 7054:7054";

    private static final String DOCKER_FIRST_PEER_PORT = "-p 7051:7051";
    private static final String DOCKER_VOLUME_PATH_TO_CHAINCODE = String.format("-v %s/go/src/github.com/hyperledger/" +
        "fabric/examples/chaincode:/opt/gopath/src/github.com/hyperledger/fabric/examples/chaincode", HOME_PATH);
    private static final String DOCKER_CORE_LOGGING_LEVEL = "-e CORE_LOGGING_LEVEL=DEBUG";
    private static final String DOCKER_CORE_PEER_ID = "-e CORE_PEER_ID=vp0";
    private static final String DOCKER_CORE_PEER_ADDRESSAUTODETECT = "-e CORE_PEER_ADDRESSAUTODETECT=true";
    private static final String DOCKER_CORE_PBFT_GENERAL_N = "-e CORE_PBFT_GENERAL_N=4";
    private static final String DOCKER_CORE_PEER_VALIDATOR_CONSENSUS_PLUGIN = "-e CORE_PEER_VALIDATOR_CONSENSUS_PLUGIN=pbft";
    private static final String DOCKER_CORE_PBFT_GENERAL_MODE = "-e CORE_PBFT_GENERAL_MODE=batch";
    private static final String DOCKER_CORE_GENERAL_TIMEOUT_REQUEST = "-e CORE_PBFT_GENERAL_TIMEOUT_REQUEST=1.5s";
    private static final String DOCKER_CORE_PBFT_GENERAL_BATCHSIZE = "-e CORE_PBFT_GENERAL_BATCHSIZE=1";
    private static final String DOCKER_CORE_PBFT_GENERAL_VIEWCHANGEPERIOD = "-e CORE_PBFT_GENERAL_VIEWCHANGEPERIOD=2";
    private static final String DOCKER_CORE_PBFT_GENERAL_TIMEOUT_NULLREQUEST = "-e CORE_PBFT_GENERAL_TIMEOUT_NULLREQUEST=2.25s";
    private static final String DOCKER_PEER_NODE_START = "hyperledger/fabric-peer peer node start";
    private static final String DOCKER_PEER_DISCOVERY_ROOTNODE = "-e CORE_PEER_DISCOVERY_ROOTNODE=";

    private static final String START_PEER = String.join(" ", DOCKER_RUN_COMMAND, DOCKER_VOLUME_SOCK,
        DOCKER_VOLUME_PATH_TO_CHAINCODE, DOCKER_CORE_LOGGING_LEVEL, DOCKER_CORE_PEER_ID, DOCKER_CORE_PEER_ADDRESSAUTODETECT,
        DOCKER_CORE_PBFT_GENERAL_N, DOCKER_CORE_PEER_VALIDATOR_CONSENSUS_PLUGIN, DOCKER_CORE_PBFT_GENERAL_MODE,
        DOCKER_CORE_GENERAL_TIMEOUT_REQUEST, DOCKER_CORE_PBFT_GENERAL_BATCHSIZE, DOCKER_CORE_PBFT_GENERAL_VIEWCHANGEPERIOD,
        DOCKER_CORE_PBFT_GENERAL_TIMEOUT_NULLREQUEST );

    private static final String START_FIRST_PEER = String.join(" ", START_PEER, DOCKER_FIRST_PEER_PORT, DOCKER_PEER_NODE_START);

    public FabricManager(String chainName, String admin, String passphrase, String memberServiceUrl, String keyValStore, 
                         String peer, boolean isInit, int validatingPeerID, String peerToConnect) {
        this.chainName = chainName;
        this.admin = admin;
        this.passphrase = passphrase;
        this.memberServiceUrl = memberServiceUrl;
        this.keyValStore = keyValStore;
        this.peer = peer;
        this.isInit = isInit;
        this.peerToConnect = peerToConnect;
        
        try {
            Runtime rt = Runtime.getRuntime();
            if (!isInit) {
                memberService = rt.exec(String.join(" ", DOCKER_RUN_COMMAND, DOCKER_VOLUME_SOCK, DOCKER_PORT_MEMBERSRVC,
                    DOCKER_RUN_FABRIC_MEMBERSRVC));
                fabricProcess = rt.exec(START_FIRST_PEER);
            } else {
                String startAnotherPeer = String.join(" ", START_PEER.replaceFirst("vp0", String.format("vp%d", validatingPeerID)),
                    DOCKER_PEER_DISCOVERY_ROOTNODE.concat(peerToConnect), DOCKER_PEER_NODE_START);
                fabricProcess = rt.exec(startAnotherPeer);
            }

            chain = new Chain(chainName);

            chain.setMemberServicesUrl(memberServiceUrl, null);

            chain.setKeyValStore(new FileKeyValStore(HOME_PATH.concat(keyValStore)));
            chain.addPeer(peer, null);

            Member registrar = chain.getMember(admin);
            if (!registrar.isEnrolled()) {
                try {
                    registrar = chain.enroll(admin, passphrase);
                } catch (EnrollmentException e) {
                    log.error("Cannot registrar admin", e);
                }
            }
            chain.setRegistrar(registrar);
            deployResponse = initChaincode();
        } catch (CertificateException | IOException | RegistrationException | EnrollmentException e) {
            log.error("Failed to init FabricManager instance", e);
        }
    }

    @Override
    public void start() {
        PrintOutput errorReported = FabricManager.getStreamWrapper(fabricProcess.getErrorStream(), "ERROR");
        PrintOutput outputMessage = FabricManager.getStreamWrapper(fabricProcess.getInputStream(), "OUTPUT");

        errorReported.start();
        outputMessage.start();
    }

    private static class PrintOutput extends Thread {
        InputStream is = null;

        PrintOutput(InputStream is, String type) {
            this.is = is;
        }

        public void run() {
            String s;
            try {
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(is));
                while ((s = br.readLine()) != null) {
                    log.info(s);
                }
            } catch (IOException ioe) {
                log.error("Failed to run PrintOutput class");
            }
        }
    }

    private static PrintOutput getStreamWrapper(InputStream is, String type) {
        return new PrintOutput(is, type);
    }
    
    @Override
    public void stop() {
        try {
            if (fabricProcess.isAlive())
                fabricProcess.destroyForcibly();
        } catch (Exception e) {
            log.error("stop method failed");
        }
    }
    
    private ChainCodeResponse initChaincode() throws EnrollmentException, RegistrationException {
        DeployRequest request = new DeployRequest();
        request.setChaincodePath(CHAINCODE_PATH);
                
        request.setArgs(new ArrayList<>(Collections.singletonList(ChaincodeFunction.INIT.name().toLowerCase())));
        
        Member member = getMember(admin, AFFILIATION);
        request.setChaincodeName(CHAINCODE_NAME);
        return member.deploy(request);
    }

    @Override
    public String sendMessage(byte[] body) {

        InvokeRequest request = new InvokeRequest();

        log.info("Sending message ".concat(new String(body, StandardCharsets.UTF_8)));
        
        request.setArgs(new ArrayList<>(Arrays.asList(ChaincodeFunction.WRITE.name().toLowerCase(), 
            new String(body, StandardCharsets.UTF_8))));
        request.setChaincodeID(deployResponse.getChainCodeID());
        request.setChaincodeName(deployResponse.getChainCodeID());

        Member member = getMember(admin, AFFILIATION);
        String transactionID = null;
        try {
            transactionID = member.invoke(request).getMessage();
        } catch (ChainCodeException e) {
            log.error("fail to send message");
        }

        return transactionID;
    }
    
    @Override
    public List<Message> getNewMessages(long timestamp) {

        List<Message> result = new ArrayList<>();
        String amountOfMessages = "0";

        ChainCodeResponse chainCodeResponse = getNewMessage(amountOfMessages);

        if (chainCodeResponse != null) {
            amountOfMessages = chainCodeResponse.getMessage();
        }

        int amount = Integer.parseInt(amountOfMessages);
        
        for (int i = 1; i <= amount; i++) {
            chainCodeResponse  = getNewMessage(Integer.toString(i));
            if (chainCodeResponse != null) {
                result.add(new Message(Integer.toString(i), chainCodeResponse.getMessage().getBytes(), true));
            }
        }
        
        log.info("getting messages started");

        result.forEach(message -> {
            try {
                String str = new String(message.getBody(), "UTF-8");
                log.info(str.concat(" "));
            } catch (UnsupportedEncodingException e) {
                log.error("Failed to convert byte array to string", e);
            }
        });
        log.info("getting messages ended");
        return result;
    }
    
    private ChainCodeResponse getNewMessage(String id) {
        QueryRequest request = new QueryRequest();
        request.setArgs(new ArrayList<>(Arrays.asList(ChaincodeFunction.READ.name().toLowerCase(), id)));
        request.setChaincodeID(deployResponse.getChainCodeID());
        request.setChaincodeName(deployResponse.getChainCodeID());
        Member member = getMember(admin, AFFILIATION);
        
        try {
            return member.query(request);
        } catch (ChainCodeException e) {
            log.error("ledger not found");
        }
        return null;
    }
    
    private Member getMember(String enrollmentId, String affiliation) {
        Member member = chain.getMember(enrollmentId);
        try {

            if (!member.isRegistered()) {
                RegistrationRequest registrationRequest = new RegistrationRequest();
                registrationRequest.setEnrollmentID(enrollmentId);
                registrationRequest.setAffiliation(affiliation);
                    member = chain.registerAndEnroll(registrationRequest);
            } else if (!member.isEnrolled()) {
                member = chain.enroll(enrollmentId, member.getEnrollmentSecret());
            }
        } catch (RegistrationException | EnrollmentException e) {
            log.error("Failed to register or enroll member", e);
        }
        return member;
    }
}
