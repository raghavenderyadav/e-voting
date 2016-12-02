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
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class FabricManager implements WalletManager {

    private final String chainName;
    private final String admin;
    private final String passphrase;
    private final String peer;
    private final String memberServiceUrl;
    private final String keyValStore;
    private String enrollID;
    private String enrollSecret;
    private final Logger logger = Logger.getLogger("MyLog");
    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(FabricManager.class.getName());
    private FileHandler fh;
    static private String CORE_PEER_DISCOVERY_ROOTNODE = "172.17.0.3:7051";

    private ChainCodeResponse deployResponse;
    
    private Process fabricProcess;
    private Process memberService;
    private Chain chain;
    private HashSet<String> users = new HashSet<>();
    private boolean isInit = false;
    private int id = 0;
    private int validatingPeerID = 0;
    private Runtime rt = Runtime.getRuntime();

    private printOutput errorReported, outputMessage;


    public FabricManager(String chainName, String admin, String passphrase, String memberServiceUrl, String keyValStore, 
                         String peer, boolean isInit, int validatingPeerID, String enrollSecret, String enrollID) {
        this.chainName = chainName;
        this.admin = admin;
        this.passphrase = passphrase;
        this.memberServiceUrl = memberServiceUrl;
        this.keyValStore = keyValStore;
        this.peer = peer;
        this.isInit = isInit;
        this.enrollID = enrollID;
        this.enrollSecret = enrollSecret;
        if (!isInit) {
            try {
                memberService = rt.exec("docker run --rm -i -v /var/run/docker.sock:/var/run/docker.sock " +
                    "-p 7054:7054 hyperledger/fabric-membersrvc membersrvc");
                fabricProcess = rt.exec("docker run --rm -i -v /var/run/docker.sock:/var/run/docker.sock -p 7051:7051 " +
                    "-v /home/mikhwall/go/src/github.com/hyperledger/fabric/examples/chaincode:/opt/gopath/src/github.com/hyperledger/fabric/examples/chaincode " +
                    "-e CORE_LOGGING_LEVEL=DEBUG -e CORE_PEER_ID=vp0 -e CORE_PEER_ADDRESSAUTODETECT=true " +
                    "-e CORE_PBFT_GENERAL_N=4 " +
                    "-e CORE_PEER_VALIDATOR_CONSENSUS_PLUGIN=pbft -e CORE_PBFT_GENERAL_MODE=batch " +
                    "-e CORE_PBFT_GENERAL_TIMEOUT_REQUEST=0.5s " +
                    "-e CORE_PBFT_GENERAL_BATCHSIZE=1 -e CORE_PBFT_GENERAL_VIEWCHANGEPERIOD=2 " +
                    "-e CORE_PBFT_GENERAL_TIMEOUT_NULLREQUEST=1s " +
                    " hyperledger/fabric-peer peer node start");

                //fabricProcess = rt.exec("docker-compose -f /home/mikhwall/docker-compose.0.yml up");

                start();
                sleep(5);
                chain = new Chain(chainName);
                
                chain.setMemberServicesUrl(memberServiceUrl, null);

                chain.setKeyValStore(new FileKeyValStore(System.getProperty("user.home") + keyValStore));
                chain.addPeer(peer, null);

                Member registrar = chain.getMember(admin);
                if (!registrar.isEnrolled()) {
                    try {
                        registrar = chain.enroll(admin, passphrase);
                    } catch (EnrollmentException e) {
                        e.printStackTrace();
                    }
                }
                chain.setRegistrar(registrar);
                sleep(5);
                deployResponse = initChaincode();
                sleep(4);

            } catch ( IOException | EnrollmentException | RegistrationException  | CertificateException e) {
                e.printStackTrace();
            }
        } else {
            try {

                fabricProcess = rt.exec(String.format("docker run --rm -i -v /var/run/docker.sock:/var/run/docker.sock " +
                    "-v /home/mikhwall/go/src/github.com/hyperledger/fabric/examples/chaincode:/opt/gopath/src/github.com/hyperledger/fabric/examples/chaincode " +
                    "-e CORE_PEER_ID=vp%d -e CORE_PEER_ADDRESSAUTODETECT=true -e CORE_PEER_DISCOVERY_ROOTNODE=%s " +
                    "-e CORE_PBFT_GENERAL_N=4 -e CORE_PEER_VALIDATOR_CONSENSUS_PLUGIN=pbft -e CORE_PBFT_GENERAL_MODE=batch " +
                    "-e CORE_PBFT_GENERAL_TIMEOUT_REQUEST=0.5s -e CORE_PBFT_GENERAL_BATCHSIZE=1 " +
                    "-e CORE_PBFT_GENERAL_VIEWCHANGEPERIOD=2 " +
                    "-e CORE_PBFT_GENERAL_TIMEOUT_NULLREQUEST=1s " +
                    " hyperledger/fabric-peer peer node start", validatingPeerID, enrollID));
                start();
                sleep(5);
                
                chain = new Chain(chainName);

                chain.setMemberServicesUrl(memberServiceUrl, null);

                chain.setKeyValStore(new FileKeyValStore(System.getProperty("user.home") + keyValStore));
                chain.addPeer(peer, null);

                Member registrar = chain.getMember(admin);
                if (!registrar.isEnrolled()) {
                    try {
                        registrar = chain.enroll(admin, passphrase);
                    } catch (EnrollmentException e) {
                        e.printStackTrace();
                    }
                }
                chain.setRegistrar(registrar);
                sleep(6);
                deployResponse = initChaincode();
                sleep(4);
            } catch (IOException | CertificateException | RegistrationException | EnrollmentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void start() {
        errorReported = FabricManager.getStreamWrapper(fabricProcess.getErrorStream(), "ERROR");
        outputMessage = FabricManager.getStreamWrapper(fabricProcess.getInputStream(), "OUTPUT");

//        errorReported.start();
//        outputMessage.start();
    }

    private static class printOutput extends Thread {
        InputStream is = null;

        printOutput(InputStream is, String type) {
            this.is = is;
        }

        public void run() {
            String s;
            try {
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(is));
                while ((s = br.readLine()) != null) {
                    System.out.println(s);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    private static printOutput getStreamWrapper(InputStream is, String type) {
        return new printOutput(is, type);
    }
    
    @Override
    public void stop() {
        try {
            if (fabricProcess.isAlive())
                fabricProcess.destroyForcibly();
        } catch (Exception e) {
            System.err.println("stop method failed");
        }
    }
    
    private ChainCodeResponse initChaincode() throws EnrollmentException, RegistrationException {
        DeployRequest request = new DeployRequest();
        request.setChaincodePath("github.com/hyperledger/fabric/examples/chaincode/go/evoting");
                
        request.setArgs(new ArrayList<>(Collections.singletonList("init")));
        
        Member member = getMember(admin, "bank_a");
        request.setChaincodeName("mycc");
        return member.deploy(request);
    }

    @Override
    public String sendMessage(byte[] body) {
        try {
            fh = new FileHandler("MyLogFile.log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (IOException e) {
            e.printStackTrace();
        }

        InvokeRequest request = new InvokeRequest();
        logger.info(new String(body, StandardCharsets.UTF_8));
        log.info("Sending message " + new String(body, StandardCharsets.UTF_8));
        request.setArgs(new ArrayList<>(Arrays.asList("write", new String(body, StandardCharsets.UTF_8))));
        request.setChaincodeID(deployResponse.getChainCodeID());
        request.setChaincodeName(deployResponse.getChainCodeID());

        Member member = getMember("admin", "bank_a");
        String transactionID = null;
        try {
            transactionID = member.invoke(request).getMessage();
            logger.info(transactionID);
        } catch (ChainCodeException e) {

        }
        
        id++;
        return transactionID;
    }
    
    private String sendMessage(String recipient, byte[] body) {
        
        users.add(recipient);
        InvokeRequest request = new InvokeRequest();

        request.setArgs(new ArrayList<>(Arrays.asList("write" , recipient, new String(body, StandardCharsets.UTF_8))));
        request.setChaincodeID(deployResponse.getChainCodeID());
        request.setChaincodeName(deployResponse.getChainCodeID());

        Member member = getMember(recipient, "bank_a");

        String transactionID = null;
        try {
            transactionID = member.invoke(request).getTransactionID();
            logger.info(transactionID);
        } catch (ChainCodeException e) {
            e.printStackTrace();
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
            result.add(new Message(Integer.toString(i), getNewMessage(Integer.toString(i)).getMessage().getBytes(), true));
        }
        log.info("getting messages started");

        result.forEach(message -> {
            try {
                String str = new String(message.getBody(), "UTF-8");
                //logger.info(str.concat(System.getProperty("line.separator")));
                log.info(str.concat(" "));
            } catch (UnsupportedEncodingException e) {

            }
        });
        log.info("getting messages ended");
        sleep(3);
        return result;
    }
    
    public ChainCodeResponse getNewMessage(String id) {
        QueryRequest request = new QueryRequest();
        request.setArgs(new ArrayList<>(Arrays.asList("read", id)));
        request.setChaincodeID(deployResponse.getChainCodeID());
        request.setChaincodeName(deployResponse.getChainCodeID());
        Member member = getMember(admin, "bank_a");
        
        try {
            return member.query(request);
        } catch (ChainCodeException e) {
            log.error("ledger not found");
        }
        return null;
    }
    
    private Member getMember(String enrollmentId, String affiliation) {
        Member member = chain.getMember(enrollmentId);
        if (!member.isRegistered()) {
            RegistrationRequest registrationRequest = new RegistrationRequest();
            registrationRequest.setEnrollmentID(enrollmentId);
            registrationRequest.setAffiliation(affiliation);
            try {
                member = chain.registerAndEnroll(registrationRequest);
            } catch (RegistrationException | EnrollmentException e) {
                e.printStackTrace();
            }
        } else if (!member.isEnrolled()) {
            try {
                member = chain.enroll(enrollmentId, member.getEnrollmentSecret());
            } catch (EnrollmentException e) {
                e.printStackTrace();
            }
        }
        return member;
    }
    
    private static void sleep(long seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws RegistrationException, CertificateException, InterruptedException, EnrollmentException {
        String peer = "grpc://" + CORE_PEER_DISCOVERY_ROOTNODE;
            FabricManager fabricManager = new FabricManager("chain1", "admin", "Xurw3yU9zI0l",
                "grpc://172.17.0.1:7054", "/test4.properties", peer, false, 0,  "fuck", "off");
        sleep(10);
            fabricManager.sendMessage("BATMAN".getBytes());
        sleep(10);

            FabricManager fabricManager1 = new FabricManager("chain1", "admin", "Xurw3yU9zI0l", "grpc://172.17.0.1:7054",
                "/test4.properties", "grpc://172.17.0.4:7051", true, 1, "5wgHK9qqYaPy", "172.17.0.3:7051");
        sleep(10);
            fabricManager1.sendMessage("SUPERMEN".getBytes());

            FabricManager fabricManager2 = new FabricManager("chain1", "admin", "Xurw3yU9zI0l", "grpc://172.17.0.1:7054",
                "/test4.properties", "grpc://172.17.0.5:7051", true, 2, "vQelbRvja7cJ", "172.17.0.4:7051");
            FabricManager fabricManager3 = new FabricManager("chain1", "admin", "Xurw3yU9zI0l", "grpc://172.17.0.1:7054",
                "/test4.properties", "grpc://172.17.0.6:7051", true, 3, "9LKqKH5peurL", "172.17.0.5:7051");
        sleep(20);
        fabricManager1.sendMessage("Jew".getBytes());
//        sleep(5);
        fabricManager.sendMessage("Deutshe".getBytes());
//        sleep(3);
        for (int i = 0; i < 4; i++) {
            fabricManager.sendMessage(String.format("OLOLO%d", i).getBytes());
        }
        //fabricManager.getNewMessages(0);
        for (int i = 0; i < 4; i++) {
            fabricManager2.sendMessage(String.format("LOL%d", i).getBytes());
        }
        ///fabricManager.getNewMessages(0);
        for (int i = 0; i < 4; i++) {
            fabricManager1.sendMessage(String.format("EBUDAK%d", i).getBytes());
        }
        fabricManager.getNewMessages(0);
        for (int i = 0; i < 4; i++) {
            fabricManager3.sendMessage(String.format("Fabruc%d", i).getBytes());
        }
        sleep(20);
        fabricManager1.getNewMessages(0);
        fabricManager.getNewMessages(0);
        fabricManager2.getNewMessages(0);
        fabricManager3.getNewMessages(0);
        
        sleep(10);
        for (int i = 3; i < 6; i++) {
            
            fabricManager.sendMessage(String.format("HOHOHO%d", i).getBytes());
        }
        
        for (int i = 3; i < 6; i++) {

            fabricManager2.sendMessage(String.format("HYILEDGER%d", i).getBytes());
        }
        for (int i = 3; i < 6; i++) {
            sleep(1);

            fabricManager1.sendMessage(String.format("DAKEB%d", i).getBytes());
        }
        for (int i = 3; i < 6; i++) {
            sleep(1);

            fabricManager3.sendMessage(String.format("FUCKBRIC%d", i).getBytes());
        }
        sleep(15);
        fabricManager1.getNewMessages(0);
        fabricManager.getNewMessages(0);
        fabricManager2.getNewMessages(0);
        fabricManager3.getNewMessages(0);
        sleep(10);
        fabricManager.stop();
        fabricManager1.stop();
        fabricManager2.stop();
        fabricManager3.stop();
    }
}
