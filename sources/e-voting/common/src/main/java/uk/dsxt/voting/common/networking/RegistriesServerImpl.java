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

package uk.dsxt.voting.common.networking;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.datamodel.*;
import uk.dsxt.voting.common.utils.HttpHelper;

@Log4j2
@Value
public class RegistriesServerImpl implements RegistriesServer {
    private final String HOLDING_URL_PART = "/holdings";
    private final String PARTICIPANTS_URL_PART = "/participants";
    private final String VOTINGS_URL_PART = "/votings";
    private final String BLACKLIST_URL_PART = "/blackList";

    public static final String EMPTY_ERROR = "Empty answer";
    public static final String INTERNAL_LOGIC_ERROR = "Internal logic exception";
    public static final String UNKNOWN_ERROR = "Unknown exception";

    String baseUrl;

    ObjectMapper mapper = new ObjectMapper();

    HttpHelper httpHelper;

    String holdingsUrl;
    String participantUrl;
    String votingsUrl;
    String blacklistUrl;


    public RegistriesServerImpl(String baseUrl, int connectionTimeout, int readTimeout) {
        this.baseUrl = baseUrl;
        holdingsUrl = String.format("%s%s", baseUrl, HOLDING_URL_PART);
        participantUrl = String.format("%s%s", baseUrl, PARTICIPANTS_URL_PART);
        votingsUrl = String.format("%s%s", baseUrl, VOTINGS_URL_PART);
        blacklistUrl = String.format("%s%s", baseUrl, BLACKLIST_URL_PART);

        httpHelper = new HttpHelper(connectionTimeout, readTimeout);
    }

    private <T> T execute(String name, String url, Class<T> clazz) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String answer = httpHelper.request(url, null, RequestType.GET);
                    T value = mapper.readValue(answer, clazz);
                    if (value != null)
                        return value;
                    log.error("{} failed. value is null. url={}", name, url);
                } catch (InternalLogicException e) {
                    log.error("{} failed. Logic exception. url={}. Reason: {}", name, url, e.getMessage());
                } catch (Exception ex) {
                    log.error("{} failed. url={}", name, url, ex);
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            log.error("{} failed. InterruptedException. url={}", name, url, e);
        }
        return null;
    }

    @Override
    public Holding[] getHoldings() {
        return execute("getHoldings", holdingsUrl, Holding[].class);
    }

    @Override
    public Participant[] getParticipants() {
        return execute("getParticipants", participantUrl, Participant[].class);
    }

    @Override
    public Voting[] getVotings() {
        return execute("getVotings", votingsUrl, Voting[].class);
    }

    @Override
    public BlockedPacket[] getBlackList() {
        return execute("getBlackList", blacklistUrl, BlockedPacket[].class);
    }
}
