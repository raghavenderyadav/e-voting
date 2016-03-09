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
import uk.dsxt.voting.common.datamodel.AnswerType;
import uk.dsxt.voting.common.domain.dataModel.Question;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.domain.nodes.AssetsHolder;
import uk.dsxt.voting.common.iso20022.jaxb.MeetingInstruction;
import uk.dsxt.voting.common.iso20022.jaxb.Vote2Choice;
import uk.dsxt.voting.common.iso20022.jaxb.Vote4;
import uk.dsxt.voting.common.iso20022.jaxb.VoteDetails2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

    public VotingInfoWeb getVoting(String votingId) {
        final Voting voting = assetsHolder.getVoting(votingId);
        if (voting == null) {
            log.error("getVoting. Couldn't find voting with id [{}].", votingId);
            return null;
        }
        BigDecimal amount = new BigDecimal(500); // TODO Get client's amount from client's details.
        return new VotingInfoWeb(voting, amount);
    }

    public boolean vote(String votingId, String clientId, String votingChoice) {
        try {
            final Voting voting = assetsHolder.getVoting(votingId);
            if (voting == null) {
                log.error("vote method failed. Couldn't find voting with id [{}]", votingId);
                return false;
            }

            VotingChoice choice = mapper.readValue(votingChoice, VotingChoice.class);
            log.debug("Vote for voting [{}] from client [{}] received.", votingId, clientId);

            Vote2Choice voteChoice = new Vote2Choice();
            List<Vote4> voteInstr = voteChoice.getVoteInstr();
            for (Map.Entry<String, QuestionChoice> entry : choice.getQuestionChoices().entrySet()) {
                Question question = null;
                for (Question q : voting.getQuestions()) {
                    if (q.getId().equals(entry.getKey())) {
                        question = q;
                        break;
                    }
                }
                if (question == null) {
                    log.error("vote method failed. Couldn't find question with id={} in votingId={}.", entry.getKey(), votingId);
                    return false;
                }
                for (Map.Entry<String, BigDecimal> answer : entry.getValue().getAnswerChoices().entrySet()) {
                    if (answer.getValue().compareTo(BigDecimal.ZERO) == 0)
                        continue;

                    Vote4 v = new Vote4();
                    if (!question.isCanSelectMultiple()) {
                        //it means that answer is one of three variants (for, against or abstain)
                        v.setIssrLabl(question.getId());
                        AnswerType type = AnswerType.getType(answer.getKey());
                        if (type == null)
                            throw new IllegalArgumentException(String.format("vote answer %s is unknown)", answer.getKey()));
                        switch (type) {
                            case FOR: {
                                v.setFor(answer.getValue());
                                break;
                            }
                            case AGAINST: {
                                v.setAgnst(answer.getValue());
                                break;
                            }
                            case ABSTAIN: {
                                v.setAbstn(answer.getValue());
                                break;
                            }
                        }
                    } else {
                        //it means that question is cumulative
                        v.setIssrLabl(answer.getKey()); //here question id in iso format is our answer id
                        v.setFor(answer.getValue()); //the only choice here is for.
                    }
                    voteInstr.add(v);
                }
            }
            VoteDetails2 voteDetails = new VoteDetails2();
            voteDetails.setVoteInstrForAgndRsltn(voteChoice);
            //TODO: get additional info and generate MeetingInstruction
            return true;
        } catch (JsonMappingException je) {
            log.error("vote method failed. Couldn't parse votingChoice JSON. votingId: {}, votingChoice: {}", votingId, votingChoice, je.getMessage());
            return false;
        } catch (Exception e) {
            log.error("vote method failed. Couldn't process votingChoice. votingId: {}, votingChoice: {}", votingId, votingChoice, e);
            return false;
        }
    }

    public QuestionWeb[] votingResults(String votingId, String clientId) {
        final Voting voting = assetsHolder.getVoting(votingId);
        if (voting == null) {
            log.debug("votingResults. Voting with id={} not found.", votingId);
            return new QuestionWeb[0];
        }
        final VoteResult clientVote = assetsHolder.getClientVote(votingId, clientId);
        if (clientVote == null) {
            log.debug("votingResults. Client vote result with id={} for client with id={} not found.", votingId, clientId);
            return new QuestionWeb[0];
        }

        List<QuestionWeb> results = new ArrayList<>();
        for (Question question : voting.getQuestions()) {
            results.add(new QuestionWeb(question, clientVote));
        }
        return results.toArray(new QuestionWeb[results.size()]);
    }

    public long getTime(String votingId) {
        return 0; // TODO Implement.
    }

    public VoteResultWeb[] getConfirmedClientVotes(String votingId) {
        List<VoteResultWeb> results = new ArrayList<>();
        final Collection<VoteResult> votes = assetsHolder.getConfirmedClientVotes(votingId);
        results.addAll(votes.stream().map(VoteResultWeb::new).collect(Collectors.toList()));
        return results.toArray(new VoteResultWeb[results.size()]);
    }
}
