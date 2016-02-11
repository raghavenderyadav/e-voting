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

package uk.dsxt.voting.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.joda.time.Instant;
import uk.dsxt.voting.common.datamodel.*;


import java.math.BigDecimal;
import java.util.*;

@Log4j2
public class VoteAggregator {

    private final Voting voting;

    private Map<Integer, Set<Integer>> availableAnswerIdsByQuestionId = new HashMap<>();

    @Data
    @AllArgsConstructor
    private static class HolderRecord {
        private final String holderId;
        private VoteResult treeResult;
        private final List<VoteResult> selfResults;
        private final List<HolderRecord> children;
        private HolderRecord parent;
        private BigDecimal nonBlockedAmount;
    }

    private Map<String, HolderRecord> results = new HashMap<>();

    private HolderRecord rootRecord;

    public VoteAggregator(Voting voting, Holding[] holdings, BlockedPacket[] blackList) {
        this.voting = voting;
        for(Question question : voting.getQuestions()) {
            Set<Integer> answerIds = new HashSet<>();
            availableAnswerIdsByQuestionId.put(question.getId(), answerIds);
            for(Answer answer : question.getAnswers()) {
                answerIds.add(answer.getId());
            }
        }
        for(Holding holding : holdings) {
            results.put(holding.getHolderId(), new HolderRecord(holding.getHolderId(), null, new ArrayList<>(), new ArrayList<>(), null, holding.getPacketSize()));
        }
        rootRecord = new HolderRecord(null, null, new ArrayList<>(), new ArrayList<>(), null, null);
        for(Holding holding : holdings) {
            HolderRecord record = results.get(holding.getHolderId());
            record.setParent(holding.getNominalHolderId() == null ? rootRecord : results.get(holding.getNominalHolderId()));
            record.getParent().getChildren().add(record);
        }
        for(BlockedPacket blockedPacket : blackList) {
            HolderRecord record = results.get(blockedPacket.getHolderId());
            if (record != null) {
                record.setNonBlockedAmount(record.getNonBlockedAmount().subtract(blockedPacket.getPacketSize()));
            }
        }
    }

    public VoteResult getResult() {
        return rootRecord.getTreeResult();
    }

    public void addVote(VoteResult voteResult, long timestamp, String signAuthorId) {
        if (!checkVote(voteResult, timestamp, signAuthorId)) {
            return;
        }
        HolderRecord record = results.get(voteResult.getHolderId());

        if (record.selfResults.size() == 0 || record.selfResults.size() == 1 && !record.selfResults.get(0).equals(voteResult)) {
            record.selfResults.add(voteResult);
            recalculateResults(record);
        }
    }

    private void recalculateResults(HolderRecord record) {
        VoteResult treeResult = new VoteResult(voting.getId(), record.getHolderId());
        for(HolderRecord child : record.getChildren()) {
            treeResult.add(child.getTreeResult());
        }
        if (record.selfResults.size() == 1) {
            VoteResult selfResult = record.selfResults.get(0);
            boolean skipSelfRecord = false;
            for(Question question : voting.getQuestions()) {
                if (treeResult.getSumQuestionAmount(question.getId()).add(selfResult.getSumQuestionAmount(question.getId())).compareTo(record.getNonBlockedAmount()) > 0) {
                    skipSelfRecord = true;
                    break;
                }
            }
            if (!skipSelfRecord) {
                treeResult.add(selfResult);
            }
        }
        record.setTreeResult(treeResult);

        HolderRecord parent = record.getParent();
        if (parent != null) {
            recalculateResults(parent);
        }
    }

    private boolean checkVote(VoteResult voteResult, long timestamp, String signAuthorId) {
        if (timestamp < voting.getStartTimestamp()) {
            log.warn("Can not add vote of holder {} to voting {}: result timestamp {} before voting begings {}",
                    voteResult.getHolderId(), voting.getName(), new Instant(timestamp), new Instant(voting.getStartTimestamp()));
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
        for(; record != null; record = record.getParent()) {
            if (record.getHolderId().equals(signAuthorId))
                return true;
        }
        log.error("Can not add vote of holder {} to voting {}: vote sign author not found",
                voteResult.getHolderId(), voting.getName(), signAuthorId);
        return false;
    }
}
