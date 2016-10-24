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

import org.hyperledger.fabric.sdk.Member;
import org.hyperledger.fabric.sdk.exception.EnrollmentException;
import org.hyperledger.fabric.sdk.Chain;
import org.hyperledger.fabric.sdk.FileKeyValStore;

import uk.dsxt.voting.common.messaging.Message;
import uk.dsxt.voting.common.networking.WalletManager;

import java.io.*;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.List;

public class FabricManager implements WalletManager {

    private final String chainName;
    private final String admin;
    private final String passphrase;
    private final List<String> peers;
    private final String memberServiceUrl;
    private final String keyValStore;
    
    private Process fabricProcess;
    private Chain chain;
    private boolean isInitialized = false;

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
                         List<String> peers) {
        this.chainName = chainName;
        this.admin = admin;
        this.passphrase = passphrase;
        this.memberServiceUrl = memberServiceUrl;
        this.keyValStore = keyValStore;
        this.peers = peers;
        chain = new Chain(chainName);
        try {
            chain.setMemberServicesUrl(memberServiceUrl, null);
            chain.setKeyValStore(new FileKeyValStore(System.getProperty("user.home") + keyValStore));
            peers.forEach(peer -> chain.addPeer(peer, null));
            Member registrar = chain.getMember(admin);
            if (!registrar.isEnrolled()) {
                registrar = chain.enroll(admin, passphrase);
            }
            chain.setRegistrar(registrar);
        } catch (CertificateException | EnrollmentException e) {
            e.printStackTrace();
        }
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
        } catch (IOException e) {
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

    @Override
    public String sendMessage(byte[] body) {
        return null;
    }
    
    @Override
    public List<Message> getNewMessages(long timestamp) {
        return null;
    }

    public static void main(String[] args) {
        List<String> peers = Collections.singletonList("grpc://172.17.0.2:7051");
        FabricManager fabricManager = new FabricManager("chain", "admin", "Xurw3yU9zI0l", "grpc://172.17.0.2:7054", 
            "/test2.properties", peers);
        
        fabricManager.start();
        try {
            Thread.sleep(10000);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        fabricManager.stop();
    }
}
