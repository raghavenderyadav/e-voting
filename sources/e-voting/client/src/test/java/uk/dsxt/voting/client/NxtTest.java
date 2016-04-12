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
import org.junit.Ignore;
import org.junit.Test;
import uk.dsxt.voting.common.nxt.NxtWalletManager;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
@Ignore
public class NxtTest {
    @Test
    public void speedTest() {
        Properties properties = new Properties();
        properties.put("nxt.jar.path", "libs/nxt.jar");
        properties.put("nxt.javaOptions", "-Xmx4096m;-Xms4096m;-XX:+UseConcMarkSweepGC");
        properties.put("nxt.apiServerPort", "7885");
        properties.put("nxt.peerServerPort", "7882");
        properties.put("nxt.dbDir", "./nxt-client-db");
        properties.put("nxt.testDbDir", "./nxt-client-db");
        properties.put("nxt.defaultPeers", "127.0.0.1;");
        properties.put("nxt.defaultTestnetPeers", "127.0.0.1;");
        properties.put("nxt.isOffline", "false");
        properties.put("nxt.isTestnet", "true");
        properties.put("nxt.timeMultiplier", "1");
        
        NxtWalletManager walletManager = new NxtWalletManager(properties, "./conf/nxt-default.properties", "00", "NXT-9PHW-CVXU-2TDY-H4878", 
            "master_password", 60000, 60000);
        
        walletManager.start();

        ScheduledExecutorService sendService = Executors.newSingleThreadScheduledExecutor();
        final long[] idx = {0}, loaded = {0};
        long start = System.currentTimeMillis();
        sendService.scheduleAtFixedRate(() -> {
            long i = idx[0]++;
            if (i % 5000 == 0)
                log.info("i={}, loaded={}, {} per sec", i, loaded[0], i * 1000 / (System.currentTimeMillis()-start));
            try {
                walletManager.sendMessage(String.format("%d_qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqfkvhjkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkqh", 
                    i).getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                log.error("sendMessage failed. idx={} loaded={} error={}", i, loaded[0], e.getMessage());
            }
        }, 100, 10, TimeUnit.MILLISECONDS);
        
        ScheduledExecutorService getService = Executors.newSingleThreadScheduledExecutor();
        getService.scheduleAtFixedRate(() -> {
            try {
                loaded[0] += walletManager.getNewMessages(0).size();
            } catch (Exception e) {
                log.error("getMessages failed. error={}", e.getMessage());
            }
        }, 1, 60, TimeUnit.SECONDS);
        
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }
        }
        
    }
}
