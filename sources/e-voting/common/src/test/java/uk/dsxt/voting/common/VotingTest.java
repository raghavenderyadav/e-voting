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

package uk.dsxt.voting.common;

import org.junit.Test;
import uk.dsxt.voting.common.domain.dataModel.*;
import uk.dsxt.voting.common.utils.InternalLogicException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class VotingTest {

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsNullId() throws InternalLogicException {
        new Voting(null, "name", 0, 0, new Question[1]).validate();
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsEmptyId() throws InternalLogicException {
        new Voting("", "name", 0, 0, new Question[1]).validate();
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsNullName() throws InternalLogicException {
        new Voting("id", null, 0, 0, new Question[1]).validate();
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsEmptyName() throws InternalLogicException {
        new Voting("id", "", 0, 0, new Question[1]).validate();
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsNullQuestions() throws InternalLogicException {
        new Voting("voting_id", "voting_name", 0, 0, null).validate();
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsEmptyQuestions() throws InternalLogicException {
        new Voting("voting_id", "voting_name", 0, 0, new Question[0]).validate();
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsNullQuestionId() throws InternalLogicException {
        final Question[] questions = new Question[1];
        questions[0] = new Question(-1, "question", new Answer[1]);
        new Voting("voting_id", "voting_name", 0, 0, questions).validate();
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsNullQuestion() throws InternalLogicException {
        final Question[] questions = new Question[1];
        questions[0] = new Question(1, null, new Answer[1]);
        new Voting("voting_id", "voting_name", 0, 0, questions).validate();
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsQuestionNullAnswers() throws InternalLogicException {
        final Question[] questions = new Question[1];
        questions[0] = new Question(1, "question", null);
        new Voting("voting_id", "voting_name", 0, 0, questions).validate();
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsQuestionEmptyAnswers() throws InternalLogicException {
        final Question[] questions = new Question[1];
        questions[0] = new Question(1, "question", new Answer[0]);
        new Voting("voting_id", "voting_name", 0, 0, questions).validate();
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsQuestionAnswersId() throws InternalLogicException {
        final Question[] questions = new Question[1];
        final Answer[] answers = new Answer[1];
        answers[0] = new Answer(-1, "answer");
        questions[0] = new Question(1, "question", answers);
        new Voting("voting_id", "voting_name", 0, 0, questions).validate();
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsQuestionAnswersName() throws InternalLogicException {
        final Question[] questions = new Question[1];
        final Answer[] answers = new Answer[1];
        answers[0] = new Answer(1, null);
        questions[0] = new Question(1, "question", answers);
        new Voting("voting_id", "voting_name", 0, 0, questions).validate();
    }


}
