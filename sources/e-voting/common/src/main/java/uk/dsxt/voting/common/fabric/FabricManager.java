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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FabricManager implements WalletManager {

    private final String chainName;
    private final String admin;
    private final String passphrase;
    private final List<String> peers;
    private final String memberServiceUrl;
    private final String keyValStore;

    ChainCodeResponse deployResponse = null;
    
    private Process fabricProcess;
    private Chain chain;
    private boolean isInitialized = false;
    private int id = 0;

    // for testing purposes
    private FabricManager() {
        chainName = null;
        admin = null;
        passphrase = null;
        peers = null;
        memberServiceUrl = null;
        keyValStore = null;
    }
    
    public FabricManager(String chainName, String admin, String passphrase, String memberServiceUrl, String keyValStore, 
                         List<String> peers) throws RegistrationException, EnrollmentException, CertificateException, InterruptedException {
        this.chainName = chainName;
        this.admin = admin;
        this.passphrase = passphrase;
        this.memberServiceUrl = memberServiceUrl;
        this.keyValStore = keyValStore;
        this.peers = peers;
        chain = new Chain(chainName);
        chain.setMemberServicesUrl(memberServiceUrl, null);
        chain.setKeyValStore(new FileKeyValStore(System.getProperty("user.home") + keyValStore));
        peers.forEach(peer -> chain.addPeer(peer, null));
        Member registrar = chain.getMember(admin);
        if (!registrar.isEnrolled()) {
            registrar = chain.enroll(admin, passphrase);
        }
        chain.setRegistrar(registrar);
    }

    @Override
    public void start() {

        Runtime rt = Runtime.getRuntime();
        FabricManager fabricManager = new FabricManager();
        printOutput errorReported, outputMessage;
        
        try {
            fabricProcess = rt.exec("docker-compose up");
            
            isInitialized = true;
            
            errorReported = fabricManager.getStreamWrapper(fabricProcess.getErrorStream(), "ERROR");
            outputMessage = fabricManager.getStreamWrapper(fabricProcess.getInputStream(), "OUTPUT");
            
            errorReported.start();
            outputMessage.start();

            TimeUnit.SECONDS.sleep(10);
            
            deployResponse = initChaincode();

        } catch (IOException | EnrollmentException | RegistrationException | InterruptedException e) {
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
        isInitialized = false;
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
        
        Member member = getMember("User1", "chain");
        request.setChaincodeName("mycc");
        return member.deploy(request);
    }
    
    @Override
    public String sendMessage(byte[] body) {
        
        InvokeRequest request = new InvokeRequest();

        request.setArgs(new ArrayList<>(Arrays.asList("write" , Integer.toString(id), new String(body, StandardCharsets.UTF_8))));
        request.setChaincodeID(deployResponse.getChainCodeID());
        request.setChaincodeName(deployResponse.getChainCodeID());
        Member member = null;
        try {
            member = getMember("User1", "chain");
        } catch (RegistrationException | EnrollmentException e) {
            e.printStackTrace();
        }

        try {
            member.invoke(request);
        } catch (ChainCodeException e) {
            e.printStackTrace();
        }
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        id++;
        
        return null;
    }
    
    @Override
    public List<Message> getNewMessages(long timestamp) {
        
        for (int i = 0; i <= id; i++) {
            System.out.println(getNewMessage(i).getMessage());
        }
        
        return null;
    }

    private ChainCodeResponse getNewMessage(int id) {
        QueryRequest request = new QueryRequest();
        request.setArgs(new ArrayList<>(Arrays.asList("read", Integer.toString(id))));
        request.setChaincodeID(deployResponse.getChainCodeID());
        request.setChaincodeName(deployResponse.getChainCodeID());
        Member member = null;
        try {
            member = getMember("User1", "chain");
        } catch (RegistrationException | EnrollmentException e) {
            e.printStackTrace();
        }
        
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            return member.query(request);
        } catch (ChainCodeException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Member getMember(String enrollmentId, String affiliation) throws RegistrationException, EnrollmentException {
        Member member = chain.getMember(enrollmentId);
        if (!member.isRegistered()) {
            RegistrationRequest registrationRequest = new RegistrationRequest();
            registrationRequest.setEnrollmentID(enrollmentId);
            registrationRequest.setAffiliation(affiliation);
            member = chain.registerAndEnroll(registrationRequest);
        } else if (!member.isEnrolled()) {
            member = chain.enroll(enrollmentId, member.getEnrollmentSecret());
        }
        return member;
    }
    
    public static void main(String[] args) throws RegistrationException, CertificateException, InterruptedException, EnrollmentException {

        List<String> peers = Collections.singletonList("grpc://172.17.0.1:7051");
        FabricManager fabricManager = new FabricManager("chain", "admin", "Xurw3yU9zI0l", "grpc://172.17.0.1:7054", 
            "/test2.properties", peers);
        
        fabricManager.start();  
        try {
            Thread.sleep(10000);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        fabricManager.sendMessage("Hello".getBytes());
        
        fabricManager.sendMessage("How are you?.".getBytes());
        
        fabricManager.getNewMessages(10);

        TimeUnit.SECONDS.sleep(10);
        
        //fabricManager.stop();
    }
}
