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
class CrossDomainFilter extends ProxyHandler {

    private static final String ORIGIN_HEADER = "Origin";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER = "Access-Control-Allow-Credentials";

    private static final Logger cspLog = LogManager.getLogger("ContentSecurityPolicy");

    private final String filter;

    public CrossDomainFilter(Handler handler, String filter) {
        super(handler);
        this.filter = filter;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (log.isTraceEnabled())
            log.trace("handle target={}", target);
        String origin = request.getHeader(ORIGIN_HEADER);
        if ("*".equals(filter) || origin != null && origin.equals(filter)) {
            response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, origin);
            response.addHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER, "true");
        } else {
            log.warn("Origin '{}' does not match filter '{}'", origin, filter);
        }

        if (target.endsWith("/cspReport")) {
            StringBuilder jb = new StringBuilder();
            try {
                BufferedReader reader = request.getReader();
                String line;
                while ((line = reader.readLine()) != null)
                    jb.append(line);
                cspLog.info("handle. cspReport. remoteAddr='{}' X-Forwarded-For='{}' userAgent='{}'  content={}",
                        request.getRemoteAddr(), request.getHeader("X-Forwarded-For"), request.getHeader("User-Agent"), jb.toString());
            } catch (Exception e) {
                log.error("handle. can not read cspReport content", e);
            }
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLength(0);
            response.setContentType(MimeTypes.Type.TEXT_PLAIN.asString());
            response.getOutputStream().print("");
            response.getOutputStream().close();
            return;
        }

        try {
            long start = System.currentTimeMillis();
            super.handle(target, baseRequest, request, response);
            if (log.isTraceEnabled())
                log.trace("handled target={} status={} in {} ms", target, response.getStatus(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error(String.format("handle failed. target=%s remoteAddr=%s", target, request.getRemoteAddr() == null ? "null" : request.getRemoteAddr()), e);
        }
    }
}
