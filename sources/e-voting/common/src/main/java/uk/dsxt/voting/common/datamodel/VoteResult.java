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

package uk.dsxt.voting.common.datamodel;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VoteResult {

    @Getter
    private final String votingId;

    @Getter
    private final String holderId;

    @Getter
    private final Map<String, VotedAnswer> answersByQuestionId = new HashMap<>();

    public VoteResult(String votingId, String holderId) {
        this.votingId = votingId;
        this.holderId = holderId;
    }

    public VoteResult(String s) {
        if (s == null)
            throw new IllegalArgumentException("VoteResult can not be created from null string");
        String[] terms = s.split(",");
        if (terms.length == 1 && s.endsWith(",")) {
            votingId = terms[0];
            holderId = null;
            return;
        } else if (terms.length < 2)
            throw new IllegalArgumentException(String.format("VoteResult can not be created from string with %d terms (%s)", terms.length, s));
        votingId = terms[0];
        holderId = terms[1].length() == 0 ? null : terms[1];
        for(int i = 2; i < terms.length; i++) {
            VotedAnswer answer = new VotedAnswer(terms[i]);
            answersByQuestionId.put(answer.getKey(), answer);
        }
    }

    public Collection<VotedAnswer> getAnswers() {
        return answersByQuestionId.values();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(votingId);
        sb.append(',');
        if (holderId != null) {
            sb.append(holderId);
        }
        for(VotedAnswer answer : answersByQuestionId.values()) {
            sb.append(',');
            sb.append(answer);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object otherObject) {
        if (!(otherObject instanceof VoteResult))
            return false;
        VoteResult other = (VoteResult)otherObject;
        if (!votingId.equals(other.getVotingId()))
            return false;
        if ((holderId == null) != (other.getHolderId() == null) || holderId != null && !holderId.equals(other.getHolderId()))
            return false;
        if (answersByQuestionId.size() != other.getAnswers().size())
            return false;
        for(VotedAnswer otherAnswer: other.getAnswers()) {
            VotedAnswer answer = answersByQuestionId.get(otherAnswer.getKey());
            if (answer == null || answer.getVoteAmount().compareTo(otherAnswer.getVoteAmount()) != 0)
                return false;
        }
        return true;
    }

    public void add(VoteResult other) {
        if (other == null)
            return;
        for(VotedAnswer otherAnswer: other.getAnswers()) {
            VotedAnswer answer = answersByQuestionId.get(otherAnswer.getKey());
            if (answer == null) {
                answersByQuestionId.put(otherAnswer.getKey(), otherAnswer);
            } else {
                answersByQuestionId.put(otherAnswer.getKey(), new VotedAnswer(answer.getQuestionId(), answer.getAnswerId(), answer.getVoteAmount().add(otherAnswer.getVoteAmount())));
            }
        }
    }

    public BigDecimal getSumQuestionAmount(int questionId) {
        return answersByQuestionId.values().stream().filter(a -> a.getQuestionId() == questionId).map(VotedAnswer::getVoteAmount).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }
}
