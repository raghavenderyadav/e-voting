package uk.dsxt.voting.common;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import uk.dsxt.voting.common.datamodel.xml.jaxb.MeetingInstruction;
import uk.dsxt.voting.common.datamodel.xml.jaxb.MeetingNotification;

import javax.xml.bind.*;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.nio.charset.Charset;

import static org.junit.Assert.assertNotNull;

public class XmlTest {
    @Test
    public void testSerialization() throws Exception {
        //deserialization
        JAXBContext miContext = JAXBContext.newInstance(MeetingInstruction.class);
        Unmarshaller miUnmarshaller = miContext.createUnmarshaller();
        String miXml = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResource("mi.xml").openStream(), Charset.forName("windows-1251"));
        StringReader miReader = new StringReader(miXml);
        MeetingInstruction mi = (MeetingInstruction) JAXBIntrospector.getValue(miUnmarshaller.unmarshal(miReader));
        assertNotNull(mi);

        JAXBContext mnContext = JAXBContext.newInstance(MeetingNotification.class);
        Unmarshaller mnUnmarshaller = mnContext.createUnmarshaller();
        String mnXml = IOUtils.toString(Thread.currentThread().getContextClassLoader().getResource("mn.xml").openStream(), Charset.forName("windows-1251"));
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
}
