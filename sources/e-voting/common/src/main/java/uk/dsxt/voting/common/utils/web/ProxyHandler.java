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

package uk.dsxt.voting.common.utils.web;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

@Log4j2
class ProxyHandler implements Handler {

    private final Handler handler;

    public ProxyHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        handler.handle(target, baseRequest, request, response);
    }

    @Override
    public void setServer(Server server) {
        handler.setServer(server);
    }

    @Override
    public Server getServer() {
        return handler.getServer();
    }

    @Override
    public void destroy() {
        handler.destroy();
    }

    @Override
    public void start() throws Exception {
        handler.start();
    }

    @Override
    public void stop() throws Exception {
        handler.stop();
    }

    @Override
    public boolean isRunning() {
        return handler.isRunning();
    }

    @Override
    public boolean isStarted() {
        return handler.isStarted();
    }

    @Override
    public boolean isStarting() {
        return handler.isStarting();
    }

    @Override
    public boolean isStopping() {
        return handler.isStopping();
    }

    @Override
    public boolean isStopped() {
        return handler.isStopped();
    }

    @Override
    public boolean isFailed() {
        return handler.isFailed();
    }

    @Override
    public void addLifeCycleListener(Listener listener) {
        handler.addLifeCycleListener(listener);
    }

    @Override
    public void removeLifeCycleListener(Listener listener) {
        handler.removeLifeCycleListener(listener);
    }
}
