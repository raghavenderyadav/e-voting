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
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for MeetingReference5 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MeetingReference5">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="MtgId" type="{}Max35Text"/>
 *         &lt;element name="IssrMtgId" type="{}Max35Text" minOccurs="0"/>
 *         &lt;element name="MtgDtAndTm" type="{}ISODateTime" minOccurs="0"/>
 *         &lt;element name="Tp" type="{}MeetingType2Code" minOccurs="0"/>
 *         &lt;element name="Clssfctn" type="{}MeetingTypeClassification1Choice" minOccurs="0"/>
 *         &lt;element name="Lctn" type="{}PostalAddress1" maxOccurs="5" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MeetingReference5", propOrder = {
    "mtgId",
    "issrMtgId",
    "mtgDtAndTm",
    "tp",
    "clssfctn",
    "lctn"
})
public class MeetingReference5 {

    @XmlElement(name = "MtgId", required = true)
    protected String mtgId;
    @XmlElement(name = "IssrMtgId")
    protected String issrMtgId;
    @XmlElement(name = "MtgDtAndTm")
    protected XMLGregorianCalendar mtgDtAndTm;
    @XmlElement(name = "Tp")
    protected MeetingType2Code tp;
    @XmlElement(name = "Clssfctn")
    protected MeetingTypeClassification1Choice clssfctn;
    @XmlElement(name = "Lctn")
    protected List<PostalAddress1> lctn;

    /**
     * Gets the value of the mtgId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMtgId() {
        return mtgId;
    }

    /**
     * Sets the value of the mtgId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMtgId(String value) {
        this.mtgId = value;
    }

    /**
     * Gets the value of the issrMtgId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIssrMtgId() {
        return issrMtgId;
    }

    /**
     * Sets the value of the issrMtgId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIssrMtgId(String value) {
        this.issrMtgId = value;
    }

    /**
     * Gets the value of the mtgDtAndTm property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getMtgDtAndTm() {
        return mtgDtAndTm;
    }

    /**
     * Sets the value of the mtgDtAndTm property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setMtgDtAndTm(XMLGregorianCalendar value) {
        this.mtgDtAndTm = value;
    }

    /**
     * Gets the value of the tp property.
     * 
     * @return
     *     possible object is
     *     {@link MeetingType2Code }
     *     
     */
    public MeetingType2Code getTp() {
        return tp;
    }

    /**
     * Sets the value of the tp property.
     * 
     * @param value
     *     allowed object is
     *     {@link MeetingType2Code }
     *     
     */
    public void setTp(MeetingType2Code value) {
        this.tp = value;
    }

    /**
     * Gets the value of the clssfctn property.
     * 
     * @return
     *     possible object is
     *     {@link MeetingTypeClassification1Choice }
     *     
     */
    public MeetingTypeClassification1Choice getClssfctn() {
        return clssfctn;
    }

    /**
     * Sets the value of the clssfctn property.
     * 
     * @param value
     *     allowed object is
     *     {@link MeetingTypeClassification1Choice }
     *     
     */
    public void setClssfctn(MeetingTypeClassification1Choice value) {
        this.clssfctn = value;
    }

    /**
     * Gets the value of the lctn property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the lctn property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLctn().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PostalAddress1 }
     * 
     * 
     */
    public List<PostalAddress1> getLctn() {
        if (lctn == null) {
            lctn = new ArrayList<PostalAddress1>();
        }
        return this.lctn;
    }

}
