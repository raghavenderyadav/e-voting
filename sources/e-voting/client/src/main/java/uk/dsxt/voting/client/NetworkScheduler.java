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

package uk.dsxt.voting.client;

import lombok.extern.log4j.Log4j2;
import org.eclipse.jetty.server.Server;
import uk.dsxt.voting.common.utils.NetworkConnector;
import uk.dsxt.voting.common.networking.WalletManager;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class NetworkScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public NetworkScheduler(String schedule, NetworkConnector... connectors) {
        if (schedule == null) {
            log.info("messagesFile not found");
            return;
        }

        Random random = new Random();
        String[] lines = schedule.split("\\r?\\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#"))
                continue;
            String[] periods = line.split(";");
            for(String period : periods) {
                if (period.isEmpty()) {
                    continue;
                }
                String[] terms = period.split("-");
                if (terms.length != 2)
                    throw new IllegalArgumentException(String.format("WalletOff schedule record can not be created from string with %d terms (%s)", terms.length, period));
                int begin = Integer.parseInt(terms[0]);
                int end = Integer.parseInt(terms[1]);
                int sec = random.nextInt(60);
                scheduler.schedule(() -> { 
                    for(NetworkConnector connector : connectors) {
                        try {
                            if (connector != null)
                                connector.stop();
                        } catch (Exception e) {
                            log.error("Can not stop network connector {}", connector.getClass(), e);
                        }
                    }
                }, begin * 60 + sec, TimeUnit.SECONDS);
                scheduler.schedule(() -> {
                    for(NetworkConnector connector : connectors) {
                        try {
                            if (connector != null)
                                connector.start();
                        } catch (Exception e) {
                            log.error("Can not start network connector {}", connector.getClass(), e);
                        }
                    }
                }, end * 60 + sec, TimeUnit.SECONDS);
            }
        }
        log.info("WalletOff schedule loaded");
    }

    public void stop() {
        scheduler.shutdown();
    }
}
