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

package uk.dsxt.voting.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.jetty.JettyHttpContainer;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;

@Log4j2
public class JettyRunner {
    public static Server run(ResourceConfig application, Properties properties, String portPropertyName) {
        return run(application, properties, portPropertyName, "*", null, null, null);
    }

    public static Server run(ResourceConfig application, Properties properties, String portPropertyName, String originFilter,
                             String aliasName, File keystoreFile, String password) {
        try {
            Integer port = Integer.valueOf(properties.getProperty(portPropertyName));
            QueuedThreadPool threadPool = new QueuedThreadPool(
                    Integer.valueOf(properties.getProperty("jetty.maxThreads")),
                    Integer.valueOf(properties.getProperty("jetty.minThreads")),
                    Integer.valueOf(properties.getProperty("jetty.idleTimeout")),
                    new ArrayBlockingQueue<>(Integer.valueOf(properties.getProperty("jetty.maxQueueSize"))));
            Server server = new Server(threadPool);
            HttpConfiguration config = new HttpConfiguration();

            if (keystoreFile != null) {
                log.info(String.format("Jetty runner %s. SSL enabled.", application.getClass()));
                SslContextFactory sslFactory = new SslContextFactory();
                sslFactory.setCertAlias(aliasName);

                String path = keystoreFile.getAbsolutePath();
                if (!keystoreFile.exists()) {
                    log.error(String.format("Couldn't load keystore file: %s", path));
                    return null;
                }
                sslFactory.setKeyStorePath(path);
                sslFactory.setKeyStorePassword(password);
                sslFactory.setKeyManagerPassword(password);
                sslFactory.setTrustStorePath(path);
                sslFactory.setTrustStorePassword(password);

                config.setSecureScheme("https");
                config.setSecurePort(port);
                config.addCustomizer(new SecureRequestCustomizer());

                ServerConnector https = new ServerConnector(server,
                        new SslConnectionFactory(sslFactory, "http/1.1"),
                        new HttpConnectionFactory(config));
                https.setPort(port);
                server.setConnectors(new Connector[]{https});
            } else {
                ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(config));
                http.setPort(port);
                server.setConnectors(new Connector[]{http});
            }

            JettyHttpContainer handler = ContainerFactory.createContainer(JettyHttpContainer.class, application);
            if (originFilter != null) {
                Handler proxyHandler = new CrossDomainFilter(handler, originFilter);
                server.setHandler(proxyHandler);
            } else {
                server.setHandler(handler);
            }
            server.start();

            while (!server.isStarted()) {
                Thread.sleep(50);
            }
            log.info(String.format("Jetty server started %s on port %d", application.getClass(), port));
            return server;
        } catch (Exception e) {
            log.error(String.format("Jetty start failed %s.", application.getClass()), e);
            return null;
        }
    }

    public static void configureMapper(ResourceConfig resourceConfig) {
        // create custom ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // create JsonProvider to provide custom ObjectMapper
        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        provider.setMapper(mapper);
        resourceConfig.register(provider);
    }
}
