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

package uk.dsxt.voting.common.demo;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.utils.web.RequestType;
import uk.dsxt.voting.common.utils.InternalLogicException;
import uk.dsxt.voting.common.utils.web.HttpHelper;

import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

@Log4j2
public class ResultsBuilderWeb implements ResultsBuilder {
    private final static String ADD_RESULT_URL_PART = "/addResult";
    private final static String ADD_VOTE_URL_PART = "/addVote";

    private final HttpHelper httpHelper;

    private final String addResultUrl;
    private final String addVoteUrl;

    public ResultsBuilderWeb(String baseUrl, int connectionTimeout, int readTimeout) {
        addResultUrl = String.format("%s%s", baseUrl, ADD_RESULT_URL_PART);
        addVoteUrl = String.format("%s%s", baseUrl, ADD_VOTE_URL_PART);

        httpHelper = new HttpHelper(connectionTimeout, readTimeout);
    }

    private void execute(String name, String url, Map<String, String> parameters) {
        try {
            httpHelper.request(url, parameters, RequestType.POST);
        } catch (ConnectException | InternalLogicException e) {
            log.error("ResultsBuilderWeb: {} failed. url={}. error={}.", name, url, e.getMessage());
        } catch (IOException e) {
            log.error(String.format("ResultsBuilderWeb: %s failed. url=%s.", name, url), e);
        }
    }

    @Override
    public void addResult(String holderId, String voteResult) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("holderId", holderId);
        parameters.put("voteResult", voteResult);
        execute("addResult", addResultUrl, parameters);
    }

    @Override
    public void addVote(String voteResult) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("voteResult", voteResult);
        execute("addVote", addVoteUrl, parameters);
    }
}
