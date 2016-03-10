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

package uk.dsxt.voting.client.web;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.joda.time.Instant;
import uk.dsxt.voting.client.datamodel.*;
import uk.dsxt.voting.common.domain.dataModel.VoteResultStatus;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Path("/api")
public class MockVotingApiResource implements VotingAPI {

    private static final Map<String, VotingWeb> votings;

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        votings = new HashMap<>();
        votings.put("voting_1", new VotingWeb("voting_1", "voting_1", Instant.now().getMillis(), Instant.now().plus(600000).getMillis(), true, false, null));
        votings.put("voting_2", new VotingWeb("voting_2", "voting_2", Instant.now().getMillis(), Instant.now().plus(100000).getMillis(), true, true, null));
        votings.put("voting_3", new VotingWeb("voting_3", "voting_3", Instant.now().getMillis(), Instant.now().plus(100000).getMillis(), true, true, null));
        votings.put("voting_4", new VotingWeb("voting_4", "voting_4", Instant.now().getMillis(), Instant.now().plus(100000).getMillis(), true, true, null));
        votings.put("voting_5", new VotingWeb("voting_5", "voting_5", Instant.now().getMillis(), Instant.now().plus(100000).getMillis(), false, false, null));
        votings.put("voting_6", new VotingWeb("voting_6", "voting_6", Instant.now().getMillis(), Instant.now().plus(100000).getMillis(), false, false, null));
    }

    @POST
    @Path("/login")
    @Produces("application/json")
    public LoginAnswerWeb login(@FormParam("login") String login, @FormParam("password") String password) {
        log.debug("login method called. login={};", login);
        return new LoginAnswerWeb(new SessionInfoWeb("Петров Иван Васильевич", "cookie_1"), null);
    }

    @POST
    @Path("/logout")
    @Produces("application/json")
    public boolean logout(@FormParam("cookie") String cookie) {
        log.debug("logout method called. cookie={};", cookie);
        return true;
    }

    @POST
    @Path("/votings")
    @Produces("application/json")
    public VotingWeb[] getVotings(@FormParam("cookie") String cookie) {
        return votings.values().toArray(new VotingWeb[votings.size()]);
    }

    @POST
    @Path("/getVoting")
    @Produces("application/json")
    public VotingInfoWeb getVoting(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        log.debug("getVoting method called. votingId={}", votingId);
        final AnswerWeb[] answers1 = new AnswerWeb[5];
        answers1[0] = new AnswerWeb("1", "answer_1_1", null);
        answers1[1] = new AnswerWeb("2", "answer_1_2", null);
        answers1[2] = new AnswerWeb("3", "answer_1_3", null);
        answers1[3] = new AnswerWeb("4", "answer_1_4", null);
        answers1[4] = new AnswerWeb("5", "answer_1_5", null);

        final AnswerWeb[] answers2 = new AnswerWeb[3];
        answers2[0] = new AnswerWeb("1", "answer_2_1", null);
        answers2[1] = new AnswerWeb("2", "answer_2_2", null);
        answers2[2] = new AnswerWeb("3", "answer_2_3", null);

        final AnswerWeb[] answers3 = new AnswerWeb[3];
        answers3[0] = new AnswerWeb("1", "answer_3_1", null);
        answers3[1] = new AnswerWeb("2", "answer_3_2", null);
        answers3[2] = new AnswerWeb("3", "answer_3_3", null);

        final QuestionWeb[] questions = new QuestionWeb[3];
        questions[0] = new QuestionWeb("1", "question_1", answers1, false, 1);
        questions[1] = new QuestionWeb("2", "question_2", answers2, false, 1);
        questions[2] = new QuestionWeb("3", "question_3", answers3, true, 1);
        return new VotingInfoWeb(questions, new BigDecimal(500), getTime(cookie, votingId));
    }

    @POST
    @Path("/vote")
    @Produces("application/json")
    public boolean vote(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId, @FormParam("votingChoice") String votingChoice) {
        try {
            log.debug("vote method called. cookie={}; votingId={}; votingChoice={}", cookie, votingId, votingChoice);
            VotingChoice choice = mapper.readValue(votingChoice, VotingChoice.class);
            for (String question : choice.getQuestionChoices().keySet()) {
                log.debug("Question: {}, Answer: {}", question, choice.getQuestionChoices().get(question));
            }
            return true;
        } catch (Exception e) {
            log.error("vote method failed. cookie={}; votingId={}; votingChoice={}", cookie, votingId, votingChoice, e);
            return false;
        }
    }

    @POST
    @Path("/votingResults")
    @Produces("application/json")
    public VotingInfoWeb votingResults(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        log.debug("votingResults method called. votingId={}", votingId);
        final AnswerWeb[] answers1 = new AnswerWeb[5];
        answers1[0] = new AnswerWeb("1", "answer_1", BigDecimal.TEN);
        answers1[1] = new AnswerWeb("2", "answer_2", BigDecimal.ONE);
        answers1[2] = new AnswerWeb("3", "answer_3", BigDecimal.TEN);
        answers1[3] = new AnswerWeb("4", "answer_4", BigDecimal.ONE);
        answers1[4] = new AnswerWeb("5", "answer_5", BigDecimal.ZERO);

        final AnswerWeb[] answers2 = new AnswerWeb[3];
        answers2[0] = new AnswerWeb("1", "yes", BigDecimal.TEN);
        answers2[1] = new AnswerWeb("2", "no", BigDecimal.ZERO);
        answers2[2] = new AnswerWeb("3", "vozderzhalsya", BigDecimal.ZERO);

        final AnswerWeb[] answers3 = new AnswerWeb[2];
        answers3[0] = new AnswerWeb("1", "yes", BigDecimal.ZERO);
        answers3[1] = new AnswerWeb("2", "no", BigDecimal.ZERO);        

        final QuestionWeb[] questions = new QuestionWeb[3];
        questions[0] = new QuestionWeb("1", "question_1_multi", answers1, true, 1);
        questions[1] = new QuestionWeb("2", "question_2_yes_no", answers2, false, 1);
        questions[2] = new QuestionWeb("3", "question_3_no_vote", answers3, false, 1);
        return new VotingInfoWeb(questions, new BigDecimal(22), -1);
    }

    @POST
    @Path("/getTime")
    @Produces("application/json")
    public long getTime(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        log.debug("getTime method called. votingId={};", votingId);
        if (votings.containsKey(votingId)) {
            return votings.get(votingId).getEndTimestamp() - Instant.now().getMillis();
        }
        return 0;
    }

    @POST
    @Path("/getConfirmedClientVotes")
    @Produces("application/json")
    public VoteResultWeb[] getConfirmedClientVotes(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        final VoteResultWeb[] results = new VoteResultWeb[5];

        results[0] = new VoteResultWeb(votingId, "Voting Name 2016", "client_1", "Dr. Watson", BigDecimal.TEN, VoteResultStatus.OK);
        results[1] = new VoteResultWeb(votingId, "Voting Name 2016", "client_2", "Mr. Drow", BigDecimal.ONE, VoteResultStatus.OK);
        results[2] = new VoteResultWeb(votingId, "Voting Name 2016", "client_3", "Mrs. Smith", BigDecimal.ZERO, VoteResultStatus.ERROR);
        results[3] = new VoteResultWeb(votingId, "Voting Name 2016", "client_4", "Mr. Zuba", BigDecimal.ZERO, VoteResultStatus.OK);
        results[4] = new VoteResultWeb(votingId, "Voting Name 2016", "client_5", "Mr. Lenin", new BigDecimal(222222.12345678), VoteResultStatus.ERROR);
        return results;
    }
}
