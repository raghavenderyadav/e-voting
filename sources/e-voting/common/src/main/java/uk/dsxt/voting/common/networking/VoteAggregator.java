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

package uk.dsxt.voting.common.networking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.joda.time.Instant;
import uk.dsxt.voting.common.domain.dataModel.*;

import java.math.BigDecimal;
import java.util.*;

@Log4j2
public class VoteAggregator {

    private final Voting voting;

    private final Map<Integer, Set<Integer>> availableAnswerIdsByQuestionId = new HashMap<>();

    private final Map<Integer, BigDecimal> multiplicatorByQuestionId = new HashMap<>();

    @Data
    @AllArgsConstructor
    private static class HolderRecord {
        private final String participantId;
        private VoteResult sumResult;
        private final List<VoteResult> results;
        private BigDecimal nonBlockedAmount;
    }

    private Map<String, HolderRecord> results = new HashMap<>();

    private VoteResult sumResult;

    public VoteAggregator(Voting voting, Client[] clients) {
        this.voting = voting;
        for(Question question : voting.getQuestions()) {
            Set<Integer> answerIds = new HashSet<>();
            availableAnswerIdsByQuestionId.put(question.getId(), answerIds);
            multiplicatorByQuestionId.put(question.getId(), new BigDecimal(question.getMultiplicator()));
            for(Answer answer : question.getAnswers()) {
                answerIds.add(answer.getId());
            }
        }
        for(Client client : clients) {
            results.put(client.getParticipantId(), new HolderRecord(client.getParticipantId(), null, new ArrayList<>(), client.getPacketSize()));
        }
        sumResult = new VoteResult(voting.getId(), null);
    }

    public VoteResult getResult() {
        return sumResult;
    }

    public boolean addVote(VoteResult voteResult, long timestamp, String signAuthorId) {
        if (!checkVote(voteResult, timestamp, signAuthorId)) {
            return false;
        }
        HolderRecord record = results.get(voteResult.getHolderId());
        record.getSumResult().add(voteResult);
        sumResult.add(voteResult);
        return true;
    }

    private boolean checkVote(VoteResult voteResult, long timestamp, String signAuthorId) {
        if (timestamp < voting.getBeginTimestamp()) {
            log.warn("Can not add vote of holder {} to voting {}: result timestamp {} before voting begings {}",
                    voteResult.getHolderId(), voting.getName(), new Instant(timestamp), new Instant(voting.getBeginTimestamp()));
            return false;
        }
        if (timestamp > voting.getEndTimestamp()) {
            log.warn("Can not add vote of holder {} to voting {}: result timestamp {} after voting ends {}",
                    voteResult.getHolderId(), voting.getName(), new Instant(timestamp), new Instant(voting.getEndTimestamp()));
            return false;
        }

        for (VotedAnswer votedAnswer : voteResult.getAnswers()) {
            Set<Integer> answerIds = availableAnswerIdsByQuestionId.get(votedAnswer.getQuestionId());
            if (answerIds == null) {
                log.warn("Can not add vote of holder {} to voting {}: question {} not found",
                        voteResult.getHolderId(), voting.getName(), votedAnswer.getQuestionId());
                return false;
            }
            if (!answerIds.contains(votedAnswer.getAnswerId())) {
                log.warn("Can not add vote of holder {} to voting {}: question {} does not contain answer {}",
                        voteResult.getHolderId(), voting.getName(), votedAnswer.getQuestionId(), votedAnswer.getAnswerId());
                return false;
            }
        }

        HolderRecord record = results.get(voteResult.getHolderId());
        if (record == null) {
            log.warn("Can not add vote of holder {} to voting {}: holder not found",
                    voteResult.getHolderId(), voting.getName());
            return false;
        }
        if (!record.getParticipantId().equals(signAuthorId)) {
            log.warn("Can not add vote of holder {} to voting {}: vote sign author is incorrect",
                    voteResult.getHolderId(), voting.getName());
            return false;
        }

        Map<Integer, BigDecimal> sumByQuestionId = new HashMap<>();
        for(VotedAnswer answer: voteResult.getAnswers()) {
            BigDecimal oldSum = sumByQuestionId.get(answer.getQuestionId());
            sumByQuestionId.put(answer.getQuestionId(), oldSum == null ? answer.getVoteAmount() : answer.getVoteAmount().add(oldSum));
        }
        for(VotedAnswer answer: voteResult.getAnswers()) {
            BigDecimal oldSum = sumByQuestionId.get(answer.getQuestionId());
            sumByQuestionId.put(answer.getQuestionId(), oldSum == null ? answer.getVoteAmount() : answer.getVoteAmount().add(oldSum));
        }
        for(VotedAnswer answer: record.getSumResult().getAnswers()) {
            BigDecimal oldSum = sumByQuestionId.get(answer.getQuestionId());
            sumByQuestionId.put(answer.getQuestionId(), oldSum == null ? answer.getVoteAmount() : answer.getVoteAmount().add(oldSum));
        }
        for(Map.Entry<Integer, BigDecimal> questionEntry : sumByQuestionId.entrySet()) {
            BigDecimal multiplicator = multiplicatorByQuestionId.get(questionEntry.getKey());
            if (multiplicator == null) {
                log.warn("Can not add vote of holder {} to voting {}: question {} not found",
                        voteResult.getHolderId(), voting.getName(), questionEntry.getKey());
                return false;
            }
            if (record.getNonBlockedAmount().multiply(multiplicator).compareTo(questionEntry.getValue()) < 0) {
                log.warn("Can not add vote of holder {} to voting {}: question {} has too many votes {}, limit is {}",
                        voteResult.getHolderId(), voting.getName(), questionEntry.getKey(), questionEntry.getValue(), record.getNonBlockedAmount().multiply(multiplicator));
                return false;
            }
        }
        return true;
    }
}
