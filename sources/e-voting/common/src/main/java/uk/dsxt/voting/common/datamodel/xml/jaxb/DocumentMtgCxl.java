//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.02.25 at 03:58:45 PM GMT+03:00 
//


package uk.dsxt.voting.common.datamodel.xml.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Document_MtgCxl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Document_MtgCxl">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="MtgCxl" type="{}MeetingCancellationV04"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Document_MtgCxl", propOrder = {
    "mtgCxl"
})
public class DocumentMtgCxl {

    @XmlElement(name = "MtgCxl", required = true)
    protected MeetingCancellationV04 mtgCxl;

    /**
     * Gets the value of the mtgCxl property.
     * 
     * @return
     *     possible object is
     *     {@link MeetingCancellationV04 }
     *     
     */
    public MeetingCancellationV04 getMtgCxl() {
        return mtgCxl;
    }

    /**
     * Sets the value of the mtgCxl property.
     * 
     * @param value
     *     allowed object is
     *     {@link MeetingCancellationV04 }
     *     
     */
    public void setMtgCxl(MeetingCancellationV04 value) {
        this.mtgCxl = value;
    }

}
