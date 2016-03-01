package uk.dsxt.voting.registriesserver;

import org.junit.BeforeClass;
import org.junit.Test;
import uk.dsxt.voting.common.datamodel.*;
import uk.dsxt.voting.common.utils.InternalLogicException;

import static org.junit.Assert.*;

public class RegistriesServerManagerTest {

    private static RegistriesServerManager manager;

    private static Participant[] participants;

    @BeforeClass
    public static void setUp() throws InternalLogicException {
        participants = new Participant[1];
        participants[0] = new Participant("id", "name", "public_key");

        final Answer[] answers = new Answer[3];
        answers[0] = new Answer(1, "answer_1");
        answers[1] = new Answer(2, "answer_2");
        answers[2] = new Answer(3, "answer_3");

        final Question[] questions = new Question[1];
        questions[0] = new Question(1, "question_1", answers);

        manager = new RegistriesServerManager(participants);
    }

    @Test
    public void testGetParticipants() {
        assertTrue(participants.length > 0);
        assertArrayEquals(manager.getParticipants(), participants);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsNull() throws InternalLogicException {
        manager.validateVotings(null);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsEmpty() throws InternalLogicException {
        manager.validateVotings(new Voting[0]);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsNullId() throws InternalLogicException {
        final Voting[] votings = new Voting[1];
        votings[0] = new Voting(null, "name", 0, 0, new Question[1]);
        manager.validateVotings(votings);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsEmptyId() throws InternalLogicException {
        final Voting[] votings = new Voting[1];
        votings[0] = new Voting("", "name", 0, 0, new Question[1]);
        manager.validateVotings(votings);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsNullName() throws InternalLogicException {
        final Voting[] votings = new Voting[1];
        votings[0] = new Voting("id", null, 0, 0, new Question[1]);
        manager.validateVotings(votings);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsEmptyName() throws InternalLogicException {
        final Voting[] votings = new Voting[1];
        votings[0] = new Voting("id", "", 0, 0, new Question[1]);
        manager.validateVotings(votings);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsNullQuestions() throws InternalLogicException {
        final Voting[] votings = new Voting[1];
        votings[0] = new Voting("voting_id", "voting_name", 0, 0, null);
        manager.validateVotings(votings);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsEmptyQuestions() throws InternalLogicException {
        final Voting[] votings = new Voting[1];
        votings[0] = new Voting("voting_id", "voting_name", 0, 0, new Question[0]);
        manager.validateVotings(votings);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsNullQuestionId() throws InternalLogicException {
        final Voting[] votings = new Voting[1];
        final Question[] questions = new Question[1];
        questions[0] = new Question(-1, "question", new Answer[1]);
        votings[0] = new Voting("voting_id", "voting_name", 0, 0, questions);
        manager.validateVotings(votings);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsNullQuestion() throws InternalLogicException {
        final Voting[] votings = new Voting[1];
        final Question[] questions = new Question[1];
        questions[0] = new Question(1, null, new Answer[1]);
        votings[0] = new Voting("voting_id", "voting_name", 0, 0, questions);
        manager.validateVotings(votings);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsQuestionNullAnswers() throws InternalLogicException {
        final Voting[] votings = new Voting[1];
        final Question[] questions = new Question[1];
        questions[0] = new Question(1, "question", null);
        votings[0] = new Voting("voting_id", "voting_name", 0, 0, questions);
        manager.validateVotings(votings);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsQuestionEmptyAnswers() throws InternalLogicException {
        final Voting[] votings = new Voting[1];
        final Question[] questions = new Question[1];
        questions[0] = new Question(1, "question", new Answer[0]);
        votings[0] = new Voting("voting_id", "voting_name", 0, 0, questions);
        manager.validateVotings(votings);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsQuestionAnswersId() throws InternalLogicException {
        final Voting[] votings = new Voting[1];
        final Question[] questions = new Question[1];
        final Answer[] answers = new Answer[1];
        answers[0] = new Answer(-1, "answer");
        questions[0] = new Question(1, "question", answers);
        votings[0] = new Voting("voting_id", "voting_name", 0, 0, questions);
        manager.validateVotings(votings);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateVotingsQuestionAnswersName() throws InternalLogicException {
        final Voting[] votings = new Voting[1];
        final Question[] questions = new Question[1];
        final Answer[] answers = new Answer[1];
        answers[0] = new Answer(1, null);
        questions[0] = new Question(1, "question", answers);
        votings[0] = new Voting("voting_id", "voting_name", 0, 0, questions);
        manager.validateVotings(votings);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsNull() throws Exception {
        manager.validateParticipants(null);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsEmpty() throws Exception {
        manager.validateParticipants(new Participant[0]);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsNullId() throws Exception {
        final Participant[] participants = new Participant[1];
        participants[0] = new Participant(null, "name", "public_key");
        manager.validateParticipants(participants);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsEmptyId() throws Exception {
        final Participant[] participants = new Participant[1];
        participants[0] = new Participant("", "name", "public_key");
        manager.validateParticipants(participants);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsNullName() throws Exception {
        final Participant[] participants = new Participant[1];
        participants[0] = new Participant("id", null, "public_key");
        manager.validateParticipants(participants);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsNullKey() throws Exception {
        final Participant[] participants = new Participant[1];
        participants[0] = new Participant("id", "name", null);
        manager.validateParticipants(participants);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsEmptyKey() throws Exception {
        final Participant[] participants = new Participant[1];
        participants[0] = new Participant("id", "name", "");
        manager.validateParticipants(participants);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsSameId() throws Exception {
        final Participant[] participants = new Participant[2];
        participants[0] = new Participant("id", "name1", "key1");
        participants[1] = new Participant("id", "name2", "key2");
        manager.validateParticipants(participants);
    }
}