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
        votings.put("voting_1", new VotingWeb("voting_1", "voting_1", Instant.now().getMillis(), Instant.now().plus(6000000).getMillis(), true, false));
        votings.put("voting_2", new VotingWeb("voting_2", "voting_2", Instant.now().getMillis(), Instant.now().plus(3000000).getMillis(), true, true));
        votings.put("voting_3", new VotingWeb("voting_3", "voting_3", Instant.now().getMillis(), Instant.now().plus(3000000).getMillis(), true, true));
        votings.put("voting_4", new VotingWeb("voting_4", "voting_4", Instant.now().getMillis(), Instant.now().plus(3000000).getMillis(), true, true));
        votings.put("voting_5", new VotingWeb("voting_5", "voting_5", Instant.now().getMillis(), Instant.now().plus(3000000).getMillis(), false, false));
        votings.put("voting_6", new VotingWeb("voting_6", "voting_6", Instant.now().getMillis(), Instant.now().plus(1000000).getMillis(), false, false));
    }

    @POST
    @Path("/login")
    @Produces("application/json")
    public RequestResult login(@FormParam("login") String login, @FormParam("password") String password) {
        try {
            log.debug("login method called. login={};", login);
            if (login.equals("admin") && password.equals("admin"))
                return new RequestResult<>(new SessionInfoWeb("Админов Админ Админович", "cookie_admin", UserRole.ADMIN), null);
            return new RequestResult<>(new SessionInfoWeb("Петров Иван Васильевич", "cookie_1", UserRole.VOTER), null);
        } catch (Exception e) {
            log.error("login method failed. login={};", login, e);
            return new RequestResult<>(APIException.UNKNOWN_EXCEPTION);
        }
    }

    @POST
    @Path("/logout")
    @Produces("application/json")
    public RequestResult logout(@FormParam("cookie") String cookie) {
        log.debug("logout method called. cookie={};", cookie);
        return new RequestResult<>(true, null);
    }

    @POST
    @Path("/votings")
    @Produces("application/json")
    public RequestResult getVotings(@FormParam("cookie") String cookie) {
        return new RequestResult<>(votings.values().toArray(new VotingWeb[votings.size()]), null);
    }

    @POST
    @Path("/getVoting")
    @Produces("application/json")
    public RequestResult getVoting(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
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
        return new RequestResult<>(new VotingInfoWeb(questions, new BigDecimal(500), (long)getTime(cookie, votingId).getResult(), null), null);
    }

    @POST
    @Path("/vote")
    @Produces("application/json")
    public RequestResult vote(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId, @FormParam("votingChoice") String votingChoice) {
        try {
            log.debug("vote method called. cookie={}; votingId={}; votingChoice={}", cookie, votingId, votingChoice);
            VotingChoice choice = mapper.readValue(votingChoice, VotingChoice.class);
            for (String question : choice.getQuestionChoices().keySet()) {
                log.debug("Question: {}, Answer: {}", question, choice.getQuestionChoices().get(question));
            }
            final AnswerWeb[] answers1 = new AnswerWeb[4];
            answers1[0] = new AnswerWeb("1", "answer_1", BigDecimal.TEN);
            answers1[1] = new AnswerWeb("2", "answer_2", BigDecimal.ONE);
            answers1[2] = new AnswerWeb("3", "answer_3", BigDecimal.TEN);
            answers1[3] = new AnswerWeb("4", "answer_4", BigDecimal.ONE);

            final AnswerWeb[] answers2 = new AnswerWeb[1];
            answers2[0] = new AnswerWeb("1", "yes", BigDecimal.TEN);

            final QuestionWeb[] questions = new QuestionWeb[3];
            questions[0] = new QuestionWeb("1", "question_1_multi", answers1, true, 1);
            questions[1] = new QuestionWeb("2", "question_2_yes_no", answers2, false, 1);
            questions[2] = new QuestionWeb("3", "question_3_no_vote", new AnswerWeb[0], false, 1);
            return new RequestResult<>(new VotingInfoWeb(questions, new BigDecimal(22), 57000L, "<root>mock xml body</root>"), null);
        } catch (Exception e) {
            log.error("vote method failed. cookie={}; votingId={}; votingChoice={}", cookie, votingId, votingChoice, e);
            return new RequestResult<>(APIException.UNKNOWN_EXCEPTION);
        }
    }

    @Override
    @Path("/signVote")
    @Produces("application/json")
    public RequestResult signVote(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId, @FormParam("isSign") Boolean isSign, @FormParam("sign") String signature) {
        try {
            log.debug("signVote method called. cookie={}; votingId={}; isSign={}; signature={}", cookie, votingId, isSign, signature);
            return new RequestResult<>(true, null);
        } catch (Exception e) {
            log.error("signVote method failed. cookie={}; votingId={}; isSign={}; signature={}", cookie, votingId, isSign, signature, e);
            return new RequestResult<>(APIException.UNKNOWN_EXCEPTION);
        }
    }

    @POST
    @Path("/votingResults")
    @Produces("application/json")
    public RequestResult votingResults(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        log.debug("votingResults method called. votingId={}", votingId);
        final AnswerWeb[] answers1 = new AnswerWeb[4];
        answers1[0] = new AnswerWeb("1", "answer_1", BigDecimal.TEN);
        answers1[1] = new AnswerWeb("2", "answer_2", BigDecimal.ONE);
        answers1[2] = new AnswerWeb("3", "answer_3", BigDecimal.TEN);
        answers1[3] = new AnswerWeb("4", "answer_4", BigDecimal.ONE);

        final AnswerWeb[] answers2 = new AnswerWeb[1];
        answers2[0] = new AnswerWeb("1", "yes", BigDecimal.TEN);

        final QuestionWeb[] questions = new QuestionWeb[3];
        questions[0] = new QuestionWeb("1", "question_1_multi", answers1, true, 1);
        questions[1] = new QuestionWeb("2", "question_2_yes_no", answers2, false, 1);
        questions[2] = new QuestionWeb("3", "question_3_no_vote", new AnswerWeb[0], false, 1);
        return new RequestResult<>(new VotingInfoWeb(questions, new BigDecimal(22), null, null), null);
    }

    @POST
    @Path("/getTime")
    @Produces("application/json")
    public RequestResult getTime(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        log.debug("getTime method called. votingId={};", votingId);
        if (votings.containsKey(votingId)) {
            return new RequestResult<>(votings.get(votingId).getEndTimestamp() - Instant.now().getMillis(), null);
        }
        return new RequestResult<>(APIException.VOTING_NOT_FOUND);
    }

    @POST
    @Path("/getConfirmedClientVotes")
    @Produces("application/json")
    public RequestResult getConfirmedClientVotes(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        final VoteResultWeb[] results = new VoteResultWeb[5];

        results[0] = new VoteResultWeb(votingId, "Voting Name 2016", "client_1", "Dr. Watson", BigDecimal.TEN, VoteResultStatus.OK);
        results[1] = new VoteResultWeb(votingId, "Voting Name 2016", "client_2", "Mr. Drow", BigDecimal.ONE, VoteResultStatus.OK);
        results[2] = new VoteResultWeb(votingId, "Voting Name 2016", "client_3", "Mrs. Smith", BigDecimal.ZERO, VoteResultStatus.ERROR);
        results[3] = new VoteResultWeb(votingId, "Voting Name 2016", "client_4", "Mr. Zuba", BigDecimal.ZERO, VoteResultStatus.OK);
        results[4] = new VoteResultWeb(votingId, "Voting Name 2016", "client_5", "Mr. Lenin", new BigDecimal(222222.12345678), VoteResultStatus.ERROR);

        return new RequestResult<>(results, null);
    }

    @POST
    @Path("/getAllClientVotes")
    @Produces("application/json")
    public RequestResult getAllClientVotes(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        final VoteResultWeb[] results = new VoteResultWeb[10];

        results[0] = new VoteResultWeb(votingId, "Voting Name 2016", "client_1", "Dr. Watson", BigDecimal.TEN, VoteResultStatus.OK);
        results[1] = new VoteResultWeb(votingId, "Voting Name 2016", "client_2", "Mr. Drow", BigDecimal.ONE, VoteResultStatus.OK);
        results[2] = new VoteResultWeb(votingId, "Voting Name 2016", "client_3", "Mrs. Smith", BigDecimal.ZERO, VoteResultStatus.ERROR);
        results[3] = new VoteResultWeb(votingId, "Voting Name 2016", "client_4", "Mr. Zuba", BigDecimal.ZERO, VoteResultStatus.OK);
        results[4] = new VoteResultWeb(votingId, "Voting Name 2016", "client_5", "Mr. Lenin", new BigDecimal(24324234), VoteResultStatus.ERROR);
        results[5] = new VoteResultWeb(votingId, "Voting Name 2016", "client_6", "Mr. Kak", BigDecimal.ONE, VoteResultStatus.OK);
        results[6] = new VoteResultWeb(votingId, "Voting Name 2016", "client_7", "Mrs. Drow", BigDecimal.ZERO, VoteResultStatus.ERROR);
        results[7] = new VoteResultWeb(votingId, "Voting Name 2016", "client_8", "Mr. Smith", BigDecimal.ZERO, VoteResultStatus.OK);
        results[8] = new VoteResultWeb(votingId, "Voting Name 2016", "client_9", "Mr. Stalin", new BigDecimal(6435674), VoteResultStatus.ERROR);
        results[9] = new VoteResultWeb(votingId, "Voting Name 2016", "client_10", "Mr. Kalinin", new BigDecimal(5632626), VoteResultStatus.OK);

        return new RequestResult<>(results, null); 
    }

    @POST
    @Path("/votingTotalResults")
    @Produces("application/json")
    public RequestResult votingTotalResults(@FormParam("cookie") String cookie, @FormParam("votingId") String votingId) {
        log.debug("votingTotalResults method called. votingId={}", votingId);
        final AnswerWeb[] answers1 = new AnswerWeb[5];
        answers1[0] = new AnswerWeb("1", "answer_1", new BigDecimal(100000));
        answers1[1] = new AnswerWeb("2", "answer_2", new BigDecimal(999999));
        answers1[2] = new AnswerWeb("3", "answer_3", new BigDecimal(777777777.77777777));
        answers1[3] = new AnswerWeb("4", "answer_4", new BigDecimal(0.42353242));
        answers1[4] = new AnswerWeb("5", "answer_5", BigDecimal.ZERO);

        final AnswerWeb[] answers2 = new AnswerWeb[3];
        answers2[0] = new AnswerWeb("1", "yes", new BigDecimal(123546547));
        answers2[1] = new AnswerWeb("2", "no", new BigDecimal(789987342.324));
        answers2[2] = new AnswerWeb("3", "vozderzhalsya", BigDecimal.ZERO);

        final AnswerWeb[] answers3 = new AnswerWeb[3];
        answers3[0] = new AnswerWeb("1", "yes", new BigDecimal(6547));
        answers3[1] = new AnswerWeb("2", "no", new BigDecimal(987342.324));
        answers3[2] = new AnswerWeb("3", "ne sderzhalsya", BigDecimal.ZERO);

        final QuestionWeb[] questions = new QuestionWeb[3];
        questions[0] = new QuestionWeb("1", "question_1_multi", answers1, true, 1);
        questions[1] = new QuestionWeb("2", "question_2_yes_no_1", answers2, false, 1);
        questions[2] = new QuestionWeb("3", "question_3_yes_no_2", answers3, false, 1);
        return new RequestResult<>(new VotingInfoWeb(questions, BigDecimal.ZERO, null, null), null);
    }
}
