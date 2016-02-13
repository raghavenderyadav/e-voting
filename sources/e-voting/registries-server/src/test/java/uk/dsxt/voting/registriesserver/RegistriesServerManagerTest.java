package uk.dsxt.voting.registriesserver;

import org.junit.BeforeClass;
import org.junit.Test;
import uk.dsxt.voting.common.datamodel.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RegistriesServerManagerTest {

    private static RegistriesServerManager manager;

    private static Participant[] participants;
    private static Holding[] holdings;
    private static Voting[] votings;
    private static BlockedPacket[] blackList;

    @BeforeClass
    public static void setUp() throws InternalLogicException {
        participants = new Participant[1];
        participants[0] = new Participant("id", "name", "public_key");

        holdings = new Holding[1];
        holdings[0] = new Holding("id", new BigDecimal("10"), null);

        final Answer[] answers = new Answer[3];
        answers[0] = new Answer(1, "answer_1");
        answers[1] = new Answer(2, "answer_2");
        answers[2] = new Answer(3, "answer_3");

        final Question[] questions = new Question[1];
        questions[0] = new Question(1, "question_1", answers);

        votings = new Voting[1];
        votings[0] = new Voting("id", "name", 1234567L, 1237777, questions);

        blackList = new BlockedPacket[1];
        blackList[0] = new BlockedPacket("id", BigDecimal.TEN);

        manager = new RegistriesServerManager(participants, holdings, votings, blackList);
    }

    @Test
    public void testGetHoldings() {
        assertTrue(holdings.length > 0);
        assertArrayEquals(manager.getHoldings(), holdings);
    }

    @Test
    public void testGetParticipants() {
        assertTrue(participants.length > 0);
        assertArrayEquals(manager.getParticipants(), participants);
    }

    @Test
    public void testGetVotings() {
        assertTrue(votings.length > 0);
        assertArrayEquals(manager.getVotings(), votings);
    }

    @Test
    public void testGetBlackList() {
        assertTrue(blackList.length > 0);
        assertArrayEquals(manager.getBlackList(), blackList);
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

    @Test
    public void testValidateAndMapParticipants() throws Exception {
        final Map<String, Participant> map = manager.validateAndMapParticipants(participants);
        assertTrue(map.size() == 1);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsNull() throws Exception {
        manager.validateAndMapParticipants(null);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsEmpty() throws Exception {
        manager.validateAndMapParticipants(new Participant[0]);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsNullId() throws Exception {
        final Participant[] participants = new Participant[1];
        participants[0] = new Participant(null, "name", "public_key");
        manager.validateAndMapParticipants(participants);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsEmptyId() throws Exception {
        final Participant[] participants = new Participant[1];
        participants[0] = new Participant("", "name", "public_key");
        manager.validateAndMapParticipants(participants);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsNullName() throws Exception {
        final Participant[] participants = new Participant[1];
        participants[0] = new Participant("id", null, "public_key");
        manager.validateAndMapParticipants(participants);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsNullKey() throws Exception {
        final Participant[] participants = new Participant[1];
        participants[0] = new Participant("id", "name", null);
        manager.validateAndMapParticipants(participants);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsEmptyKey() throws Exception {
        final Participant[] participants = new Participant[1];
        participants[0] = new Participant("id", "name", "");
        manager.validateAndMapParticipants(participants);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapParticipantsSameId() throws Exception {
        final Participant[] participants = new Participant[2];
        participants[0] = new Participant("id", "name1", "key1");
        participants[1] = new Participant("id", "name2", "key2");
        manager.validateAndMapParticipants(participants);
    }

    @Test
    public void testValidateAndMapHoldings() throws Exception {
        final Map<String, Participant> participantsMap = manager.validateAndMapParticipants(participants);
        final Map<String, BigDecimal> map = manager.validateAndMapHoldings(holdings, participantsMap);
        assertNotNull(map);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapHoldingsNull() throws Exception {
        final Map<String, Participant> participantsMap = manager.validateAndMapParticipants(participants);
        manager.validateAndMapHoldings(null, participantsMap);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapHoldingsEmpty() throws Exception {
        final Map<String, Participant> participantsMap = manager.validateAndMapParticipants(participants);
        manager.validateAndMapHoldings(new Holding[0], participantsMap);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapHoldingsHolderIdNull() throws Exception {
        final Map<String, Participant> participantsMap = manager.validateAndMapParticipants(participants);
        final Holding[] holdings = new Holding[1];
//        holdings[0] = new Holding("holder_id", BigDecimal.ONE, "nominal_holder_id");
        holdings[0] = new Holding(null, BigDecimal.ONE, "nominal_holder_id");
        final Map<String, BigDecimal> map = manager.validateAndMapHoldings(holdings, participantsMap);
        assertNotNull(map);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapHoldingsHolderIdEmpty() throws Exception {
        final Map<String, Participant> participantsMap = manager.validateAndMapParticipants(participants);
        final Holding[] holdings = new Holding[1];
        holdings[0] = new Holding("", BigDecimal.ONE, "nominal_holder_id");
        final Map<String, BigDecimal> map = manager.validateAndMapHoldings(holdings, participantsMap);
        assertNotNull(map);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapHoldingsHolderPacketSize() throws Exception {
        final Map<String, Participant> participantsMap = manager.validateAndMapParticipants(participants);
        final Holding[] holdings = new Holding[1];
        holdings[0] = new Holding("id", BigDecimal.valueOf(-1), "nominal_holder_id");
        final Map<String, BigDecimal> map = manager.validateAndMapHoldings(holdings, participantsMap);
        assertNotNull(map);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapHoldingsHolderSameId() throws Exception {
        final Map<String, Participant> participantsMap = manager.validateAndMapParticipants(participants);
        final Holding[] h = new Holding[1];
        h[0] = new Holding("id1", BigDecimal.ONE, "nominal_holder_id");
        final Map<String, BigDecimal> map = manager.validateAndMapHoldings(h, participantsMap);
        assertNotNull(map);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapHoldingsHolderNoParticipant() throws Exception {
        final Holding[] h = new Holding[1];
        h[0] = new Holding("id", BigDecimal.ONE, "nominal_holder_id");
        final Map<String, BigDecimal> map = manager.validateAndMapHoldings(h, new HashMap<>());
        assertNotNull(map);
    }

    @Test
    public void testValidateAndMapHoldingsNullNominalHolder() throws Exception {
        final Map<String, Participant> participantsMap = manager.validateAndMapParticipants(participants);
        final Holding[] h = new Holding[1];
        h[0] = new Holding("id", BigDecimal.ONE, null);
        final Map<String, BigDecimal> map = manager.validateAndMapHoldings(h, participantsMap);
        assertNotNull(map);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapHoldingsEmptyNominalHolder() throws Exception {
        final Map<String, Participant> participantsMap = manager.validateAndMapParticipants(participants);
        final Holding[] h = new Holding[1];
        h[0] = new Holding("id", BigDecimal.ONE, "");
        final Map<String, BigDecimal> map = manager.validateAndMapHoldings(h, participantsMap);
        assertNotNull(map);
    }

    @Test(expected=InternalLogicException.class)
    public void testValidateAndMapHoldingsSameHolder() throws Exception {
        final Map<String, Participant> participantsMap = manager.validateAndMapParticipants(participants);
        final Holding[] h = new Holding[2];
        h[0] = new Holding("id", BigDecimal.ONE, null);
        h[1] = new Holding("id", BigDecimal.TEN, null);
        final Map<String, BigDecimal> map = manager.validateAndMapHoldings(h, participantsMap);
        assertNotNull(map);
    }
}