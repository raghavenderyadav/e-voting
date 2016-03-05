package uk.dsxt.voting.registriesserver;

import org.junit.BeforeClass;
import org.junit.Test;
import uk.dsxt.voting.common.domain.dataModel.Answer;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.dataModel.Question;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.utils.InternalLogicException;

import static org.junit.Assert.*;

public class RegistriesServerManagerTest {

    private static RegistriesServerManager manager;

    private static Participant[] participants;

    @BeforeClass
    public static void setUp() throws InternalLogicException {
        participants = new Participant[1];
        participants[0] = new Participant("id", "name", "public_key");
        manager = new RegistriesServerManager(participants);
    }

    @Test
    public void testGetParticipants() {
        assertTrue(participants.length > 0);
        assertArrayEquals(manager.getParticipants(), participants);
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