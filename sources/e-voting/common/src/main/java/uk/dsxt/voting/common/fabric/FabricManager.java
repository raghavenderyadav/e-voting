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

public class FabricManager implements WalletManager {

    private final String chainName;
    private final String admin;
    private final String passphrase;
    private final List<String> peers;
    private final String memberServiceUrl;
    private final String keyValStore;

    private ChainCodeResponse deployResponse;
    
    private Process fabricProcess;
    private Chain chain;
    private HashSet<String> users = new HashSet<>();
    
    public FabricManager(String chainName, String admin, String passphrase, String memberServiceUrl, String keyValStore, 
                         List<String> peers) {
        this.chainName = chainName;
        this.admin = admin;
        this.passphrase = passphrase;
        this.memberServiceUrl = memberServiceUrl;
        this.keyValStore = keyValStore;
        this.peers = peers;
    }

    @Override
    public void start() {

        Runtime rt = Runtime.getRuntime();
        FabricManager fabricManager = new FabricManager(chainName, admin, passphrase, memberServiceUrl, keyValStore, peers);
        printOutput errorReported, outputMessage;
        
        try {

            fabricProcess = rt.exec("docker-compose up");
            
            errorReported = fabricManager.getStreamWrapper(fabricProcess.getErrorStream(), "ERROR");
            outputMessage = fabricManager.getStreamWrapper(fabricProcess.getInputStream(), "OUTPUT");
            
            errorReported.start();
            outputMessage.start();

            sleep(20);
            
            chain = new Chain(chainName);
            
            chain.setMemberServicesUrl(memberServiceUrl, null);

            chain.setKeyValStore(new FileKeyValStore(System.getProperty("user.home") + keyValStore));
            peers.forEach(peer -> chain.addPeer(peer, null));
            
            Member registrar = chain.getMember(admin);
            if (!registrar.isEnrolled()) {
                try {
                    registrar = chain.enroll(admin, passphrase);
                } catch (EnrollmentException e) {
                    e.printStackTrace();
                }
            }
            chain.setRegistrar(registrar);
            
            sleep(10);
            
            deployResponse = initChaincode();

        } catch (IOException | EnrollmentException | RegistrationException | CertificateException e) {
            e.printStackTrace();
        }
    }

    private class printOutput extends Thread {
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

    private printOutput getStreamWrapper(InputStream is, String type) {
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
        users.add(admin);
        return member.deploy(request);
    }
    
    @Override
    public String sendMessage(byte[] body) {
        return sendMessage(admin, body);
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
        } catch (ChainCodeException e) {
            e.printStackTrace();
        }
        
        sleep(10);
        
        return transactionID;
    }
    
    @Override
    public List<Message> getNewMessages(long timestamp) {
        
        List<Message> result = new ArrayList<>();
        users.forEach(user -> result.add(new Message(user, getNewMessage(user).getMessage().getBytes(), true)));
        
        result.forEach(message -> {
            try {
                System.out.println(new String(message.getBody(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
        
        return result;
    }

    public ChainCodeResponse getNewMessage(String user) {
        QueryRequest request = new QueryRequest();
        request.setArgs(new ArrayList<>(Arrays.asList("read", user)));
        request.setChaincodeID(deployResponse.getChainCodeID());
        request.setChaincodeName(deployResponse.getChainCodeID());
        Member member = getMember("admin", "bank_a");

        sleep(10);

        try {
            return member.query(request);
        } catch (ChainCodeException e) {
            e.printStackTrace();
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

        List<String> peers = Collections.singletonList("grpc://172.17.0.1:7051");
        FabricManager fabricManager = new FabricManager("chain", "admin", "Xurw3yU9zI0l", "grpc://172.17.0.1:7054", 
            "/test4.properties", peers);
        
        sleep(5);
        fabricManager.start();
        
        sleep(10);
        
        fabricManager.sendMessage("user01", "Hello".getBytes());
        fabricManager.sendMessage("user01", "Hi!".getBytes());
        
        fabricManager.sendMessage("user02", "How are you?".getBytes());
        
        fabricManager.sendMessage("I'm fine".getBytes());
        
        System.out.println(fabricManager.getNewMessage("user01").getMessage());
        fabricManager.getNewMessages(0);

        sleep(10);
        
        fabricManager.stop();
    }
}
