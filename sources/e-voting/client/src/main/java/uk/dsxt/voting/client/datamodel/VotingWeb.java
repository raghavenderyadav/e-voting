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

package uk.dsxt.voting.client.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import org.joda.time.Instant;
import uk.dsxt.voting.common.domain.dataModel.Voting;

import java.util.Arrays;
import java.util.stream.Collectors;

@Value
public class VotingWeb {
    String id;
    String name;
    long beginTimestamp;
    long endTimestamp;
    boolean isActive;
    boolean canVote;
    QuestionWeb[] questions;

    @JsonCreator
    public VotingWeb(@JsonProperty("id") String id, @JsonProperty("name") String name,
                     @JsonProperty("beginTimestamp") long beginTimestamp, @JsonProperty("endTimestamp") long endTimestamp,
                     @JsonProperty("isActive") boolean isActive, @JsonProperty("canVote") boolean canVote,
                     @JsonProperty("questions") QuestionWeb[] questions) {
        this.id = id;
        this.name = name;
        this.beginTimestamp = beginTimestamp;
        this.endTimestamp = endTimestamp;
        this.isActive = isActive;
        this.canVote = canVote;
        this.questions = questions;
    }

    public VotingWeb(Voting v) {
        this.id = v.getId();
        this.name = v.getName();
        this.beginTimestamp = v.getBeginTimestamp();
        this.endTimestamp = v.getEndTimestamp();
        final Instant now = Instant.now();
        this.isActive = now.isAfter(beginTimestamp) && now.isBefore(endTimestamp);
        this.canVote = true; // TODO
        this.questions = Arrays.stream(v.getQuestions()).map(QuestionWeb::new).collect(Collectors.toList()).toArray(new QuestionWeb[v.getQuestions().length]);
    }
}
