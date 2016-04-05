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

package uk.dsxt.voting.common.domain.dataModel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class Question {
    String id;
    String question;
    Answer[] answers;
    boolean canSelectMultiple;
    int multiplicator;

    public Question(String id, String question, Answer[] answers) {
        this.id = id;
        this.question = question;
        this.answers = answers;
        this.canSelectMultiple = false;
        this.multiplicator = 1;
    }

    @JsonCreator
    public Question(@JsonProperty("id") String id, @JsonProperty("question") String question, @JsonProperty("answers") Answer[] answers,
                    @JsonProperty("canSelectMultiple") boolean canSelectMultiple, @JsonProperty("multiplicator") int multiplicator) {
        this.id = id;
        this.question = question;
        this.answers = answers;
        this.canSelectMultiple = canSelectMultiple;
        this.multiplicator = multiplicator;
    }
}
