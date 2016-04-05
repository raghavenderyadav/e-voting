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

package uk.dsxt.voting.common.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.domain.dataModel.*;
import uk.dsxt.voting.common.utils.InternalLogicException;

import java.io.IOException;

@Log4j2
public class SimpleSerializer implements MessagesSerializer {

    private final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public String serialize(Voting voting) {
        try {
            return mapper.writeValueAsString(voting);
        } catch (JsonProcessingException e) {
            log.error("serialize Voting failed", e);
            return null;
        }
    }

    @Override
    public Voting deserializeVoting(String message) throws InternalLogicException {
        try {
            return mapper.readValue(message, Voting.class);
        } catch (IOException e) {
            throw new InternalLogicException("deserializeVoting failed: " + e.getMessage());
        }
    }

    @Override
    public String serialize(VoteResult voteResult, Voting voting) throws InternalLogicException {
        return voteResult.toString();
    }

    @Override
    public VoteResult deserializeVoteResult(String message) throws InternalLogicException {
        return new VoteResult(message);
    }
    
    @Override
    public String serialize(VoteStatus voteStatus) {
        return voteStatus.toString();
    }

    @Override
    public VoteStatus deserializeVoteStatus(String message) throws InternalLogicException {
        return new VoteStatus(message);
    }
}
