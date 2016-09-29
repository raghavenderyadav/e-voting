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

package uk.dsxt.voting.common.domain.dataModel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import uk.dsxt.voting.common.utils.InternalLogicException;

@Value
public class Voting {
    String id;
    String name;
    String type;
    long beginTimestamp;
    long endTimestamp;
    Question[] questions;
    String security;

    @JsonCreator
    public Voting(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("type") String type, @JsonProperty("beginTimestamp") long beginTimestamp, @JsonProperty("endTimestamp") long endTimestamp,
                  @JsonProperty("questions") Question[] questions, @JsonProperty("security") String security) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.beginTimestamp = beginTimestamp;
        this.endTimestamp = endTimestamp;
        this.questions = questions;
        this.security = security;
    }

    public void validate() throws InternalLogicException {
        if (id == null || id.isEmpty() || name == null || name.isEmpty() || type == null || type.isEmpty() ||  questions == null || questions.length == 0)
            throw new InternalLogicException(String.format("validateVotings failed. One of the voting fields are incorrect"));

        for (int j = 0; j < questions.length; j++) {
            Question q = questions[j];
            if (q == null || q.getQuestion() == null || q.getAnswers() == null || q.getAnswers().length == 0)
                throw new InternalLogicException(String.format("validateVotings failed. One of the question fields are incorrect. question index=%d", j));

            for (int k = 0; k < q.getAnswers().length; k++) {
                Answer a = q.getAnswers()[k];
                if (a == null || a.getName() == null)
                    throw new InternalLogicException(String.format("validateVotings failed. One of the answer fields. question index=%d, answer index=%d", j, k));
            }
        }
    }
}
