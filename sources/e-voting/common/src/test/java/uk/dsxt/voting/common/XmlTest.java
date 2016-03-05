package uk.dsxt.voting.common;

import org.junit.Test;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.iso20022.Iso20022Serializer;
import uk.dsxt.voting.common.iso20022.jaxb.MeetingInstruction;
import uk.dsxt.voting.common.iso20022.jaxb.MeetingNotification;
import uk.dsxt.voting.common.utils.PropertiesHelper;

import javax.xml.bind.*;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class XmlTest {
    @Test
    public void testJAXBSerialization() throws Exception {
        //deserialization
        JAXBContext miContext = JAXBContext.newInstance(MeetingInstruction.class);
        Unmarshaller miUnmarshaller = miContext.createUnmarshaller();
        String miXml = PropertiesHelper.getResourceString("mi.xml","windows-1251");
        StringReader miReader = new StringReader(miXml);
        MeetingInstruction mi = (MeetingInstruction) JAXBIntrospector.getValue(miUnmarshaller.unmarshal(miReader));
        assertNotNull(mi);

        JAXBContext mnContext = JAXBContext.newInstance(MeetingNotification.class);
        Unmarshaller mnUnmarshaller = mnContext.createUnmarshaller();
        String mnXml = PropertiesHelper.getResourceString("mn.xml","windows-1251");
        Source mnSource = new StreamSource(new StringReader(mnXml));
        JAXBElement<MeetingNotification> mnRoot = mnUnmarshaller.unmarshal(mnSource, MeetingNotification.class);
        MeetingNotification mn = mnRoot.getValue();
        assertNotNull(mn);

        //serialization
        JAXBContext context = JAXBContext.newInstance(MeetingInstruction.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        //m.setProperty(Marshaller.JAXB_ENCODING, "windows-1251");
        m.marshal(mi, System.out);

        context = JAXBContext.newInstance(MeetingInstruction.class);
        m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        //m.setProperty(Marshaller.JAXB_ENCODING, "windows-1251");
        m.marshal(mn, System.out);
    }

    @Test
    public void testVotingSerialization() throws Exception {
        //deserialization from xml file
        Iso20022Serializer serializer = new Iso20022Serializer();
        String votingXml = PropertiesHelper.getResourceString("voting_simple.xml","windows-1251");
        Voting voting = serializer.deserializeVoting(votingXml);
        assertNotNull(voting);

        assertEquals("123456",voting.getId());
        assertNotNull(voting.getName());

        assertEquals(4, voting.getQuestions().length);
        assertEquals("1.1", voting.getQuestions()[0].getId());
        assertEquals(3, voting.getQuestions()[0].getAnswers().length);
        assertEquals("1.2", voting.getQuestions()[1].getId());
        assertEquals(3, voting.getQuestions()[1].getAnswers().length);
        assertEquals("2.1", voting.getQuestions()[2].getId());
        assertEquals(3, voting.getQuestions()[2].getAnswers().length);
        assertEquals("3.1", voting.getQuestions()[3].getId());
        assertEquals(3, voting.getQuestions()[3].getAnswers().length);

        //serialization result to string
        String result = serializer.serialize(voting);
        assertNotNull(result);
        System.out.println(result);

        //deserialization from serialized object
        Voting serializedVoting = serializer.deserializeVoting(result);
        assertNotNull(serializedVoting);
        //check that resulted object equals voting deserialized from file
        assertEquals(voting, serializedVoting);
    }

    @Test
    public void testVoteResultSerialization() throws Exception {
        //deserialization from xml file
        Iso20022Serializer serializer = new Iso20022Serializer();
        String voteResultXml = PropertiesHelper.getResourceString("voteResult_simple.xml","windows-1251");
        VoteResult voteResult = serializer.deserializeVoteResult(voteResultXml);
        assertNotNull(voteResult);

        assertEquals("МХ1",voteResult.getHolderId());
        assertEquals("000001",voteResult.getVotingId());

        assertEquals(4, voteResult.getAnswers().size());
        /*assertEquals("1.1", voting.getQuestions()[0].getId());
        assertEquals(3, voting.getQuestions()[0].getAnswers().length);
        assertEquals("1.2", voting.getQuestions()[1].getId());
        assertEquals(3, voting.getQuestions()[1].getAnswers().length);
        assertEquals("2.1", voting.getQuestions()[2].getId());
        assertEquals(3, voting.getQuestions()[2].getAnswers().length);
        assertEquals("3.1", voting.getQuestions()[3].getId());
        assertEquals(3, voting.getQuestions()[3].getAnswers().length);*/

        //serialization result to string
        String result = serializer.serialize(voteResult);
        assertNotNull(result);
        System.out.println(result);

        //deserialization from serialized object
        VoteResult serializedVoteResult = serializer.deserializeVoteResult(result);
        assertNotNull(serializedVoteResult);
        //check that resulted object equals voting deserialized from file
        assertEquals(voteResult, serializedVoteResult);
    }
}
