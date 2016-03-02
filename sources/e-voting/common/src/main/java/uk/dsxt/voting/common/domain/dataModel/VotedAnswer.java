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

import lombok.Getter;

import java.math.BigDecimal;

public class VotedAnswer {

    @Getter
    private final int questionId;

    @Getter
    private final int answerId;

    @Getter
    private final BigDecimal voteAmount;

    @Override
    public String toString() {
        return String.format("%d-%d-%s", questionId, answerId, voteAmount);
    }

    public VotedAnswer(String s) {
        if (s == null)
            throw new IllegalArgumentException("VotedAnswer can not be created from null string");
        String[] terms = s.split("-");
        if (terms.length != 3)
            throw new IllegalArgumentException(String.format("VotedAnswer can not be created from string with %d terms (%s)", terms.length, s));
        questionId = Integer.parseInt(terms[0]);
        answerId = Integer.parseInt(terms[1]);
        voteAmount = new BigDecimal(terms[2]);
    }


    public VotedAnswer(int questionId, int answerId, BigDecimal voteAmount) {
        this.questionId = questionId;
        this.answerId = answerId;
        this.voteAmount = voteAmount;
    }

    public String getKey() {
        return String.format("%d-%d", questionId, answerId);
    }

}
