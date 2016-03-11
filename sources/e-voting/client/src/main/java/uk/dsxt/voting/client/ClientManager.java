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

package uk.dsxt.voting.client;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.client.datamodel.*;
import uk.dsxt.voting.common.domain.dataModel.Question;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.domain.nodes.AssetsHolder;
import uk.dsxt.voting.common.iso20022.jaxb.MeetingInstruction;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Value
public class ClientManager {
    private final ObjectMapper mapper = new ObjectMapper();

    MeetingInstruction participantsXml;

    AssetsHolder assetsHolder;

    public ClientManager(AssetsHolder assetsHolder, MeetingInstruction participantsXml) {
        this.assetsHolder = assetsHolder;
        this.participantsXml = participantsXml;
    }

    public VotingWeb[] getVotings() {
        final Collection<Voting> votings = assetsHolder.getVotings();
        return votings.stream().map(VotingWeb::new).collect(Collectors.toList()).toArray(new VotingWeb[votings.size()]);
    }

    public VotingInfoWeb getVoting(String votingId, String clientId) {
        final Voting voting = assetsHolder.getVoting(votingId);
        if (voting == null) {
            log.error("getVoting. Couldn't find voting with id [{}].", votingId);
            return null;
        }
        BigDecimal amount = assetsHolder.getClientPacketSize(votingId, clientId);
        return new VotingInfoWeb(voting, amount, getTime(votingId));
    }

    public boolean vote(String votingId, String clientId, String votingChoice) {
        try {
            final Voting voting = assetsHolder.getVoting(votingId);
            if (voting == null) {
                log.error("vote method failed. Couldn't find voting with id [{}]", votingId);
                return false;
            }
            
            BigDecimal packetSize = assetsHolder.getClientPacketSize(votingId, clientId);
            if (packetSize == null) {
                log.error("vote method failed. Client not found or can not vote. votingId [{}] clientId [{}]", votingId, clientId);
                return false;
            }

            VotingChoice choice = mapper.readValue(votingChoice, VotingChoice.class);
            log.debug("Vote for voting [{}] from client [{}] received.", votingId, clientId);

            VoteResult result = new VoteResult(votingId, clientId, packetSize);
            for (Map.Entry<String, QuestionChoice> entry : choice.getQuestionChoices().entrySet()) {
                Optional<Question> question = Arrays.stream(voting.getQuestions()).filter(q -> q.getId().equals(entry.getKey())).findAny();
                if (!question.isPresent()) {
                    log.error("vote method failed. Couldn't find question with id={} in votingId={}.", entry.getKey(), votingId);
                    return false;
                }
                for (Map.Entry<String, BigDecimal> answer : entry.getValue().getAnswerChoices().entrySet()) {
                    if (answer.getValue().signum() == 0)
                        continue;
                    result.setAnswer(question.get().getId(), answer.getKey(), answer.getValue());
                }
            }
            assetsHolder.addClientVote(result);
            return true;
        } catch (JsonMappingException je) {
            log.error("vote method failed. Couldn't parse votingChoice JSON. votingId: {}, votingChoice: {}", votingId, votingChoice, je.getMessage());
            return false;
        } catch (Exception e) {
            log.error("vote method failed. Couldn't process votingChoice. votingId: {}, votingChoice: {}", votingId, votingChoice, e);
            return false;
        }
    }

    public VotingInfoWeb votingResults(String votingId, String clientId) {
        final Voting voting = assetsHolder.getVoting(votingId);
        if (voting == null) {
            log.debug("votingResults. Voting with id={} not found.", votingId);
            return null;
        }
        final VoteResult clientVote = assetsHolder.getClientVote(votingId, clientId);
        if (clientVote == null) {
            log.debug("votingResults. Client vote result with id={} for client with id={} not found.", votingId, clientId);
            return null;
        }

        List<QuestionWeb> results = new ArrayList<>();
        for (Question question : voting.getQuestions()) {
            results.add(new QuestionWeb(question, clientVote));
        }
        return new VotingInfoWeb(results.toArray(new QuestionWeb[results.size()]), assetsHolder.getClientPacketSize(votingId, clientId), 0);
    }

    public long getTime(String votingId) {
        final Voting voting = assetsHolder.getVoting(votingId);
        if (voting == null) {
            log.debug("votingResults. Voting with id={} not found.", votingId);
            return -1;
        }
        long now = System.currentTimeMillis();
        if (now < voting.getBeginTimestamp() || now > voting.getEndTimestamp())
            return -1;
        return voting.getEndTimestamp() - now;
    }

    public VoteResultWeb[] getConfirmedClientVotes(String votingId) {
        List<VoteResultWeb> results = new ArrayList<>();
        final Collection<VoteResult> votes = assetsHolder.getConfirmedClientVotes(votingId);
        results.addAll(votes.stream().map(VoteResultWeb::new).collect(Collectors.toList()));
        return results.toArray(new VoteResultWeb[results.size()]);
    }

    public VoteResultWeb[] getAllClientVotes(String votingId) {
        List<VoteResultWeb> results = new ArrayList<>();
        final Collection<VoteResult> votes = assetsHolder.getAllClientVotes(votingId);
        results.addAll(votes.stream().map(VoteResultWeb::new).collect(Collectors.toList()));
        return results.toArray(new VoteResultWeb[results.size()]);
    }

    public VotingInfoWeb votingTotalResults(String votingId) {
        final Voting voting = assetsHolder.getVoting(votingId);
        if (voting == null) {
            log.debug("votingTotalResults. Voting with id={} not found.", votingId);
            return null;
        }
        final VoteResult voteResults = assetsHolder.getTotalVotingResult(votingId);
        if (voteResults == null) {
            log.debug("votingTotalResults. Total results for voting with id={} not found.", votingId);
            return null;
        }

        List<QuestionWeb> results = new ArrayList<>();
        for (Question question : voting.getQuestions()) {
            results.add(new QuestionWeb(question, voteResults));
        }
        return new VotingInfoWeb(results.toArray(new QuestionWeb[results.size()]), BigDecimal.ZERO, 0);
    }
}
