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

import uk.dsxt.voting.common.messaging.Message;
import uk.dsxt.voting.common.networking.WalletManager;

import java.io.*;
import java.util.List;

public class FabricManager implements WalletManager {

    private Process fabricProcess; 
    private boolean isInitialized = false;

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
        FabricManager fabricManager = new FabricManager();
        fabricManager.start();
        try {
            Thread.sleep(100000);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        fabricManager.stop();
    }
}
