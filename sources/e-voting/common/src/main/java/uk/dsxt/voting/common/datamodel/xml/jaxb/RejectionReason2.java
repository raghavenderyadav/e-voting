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


/**
 * <p>Java class for RejectionReason2 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RejectionReason2">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="RjctgPtyRsn" type="{}Max35Text"/>
 *         &lt;element name="RjctnDtTm" type="{}ISODateTime" minOccurs="0"/>
 *         &lt;element name="ErrLctn" type="{}Max350Text" minOccurs="0"/>
 *         &lt;element name="RsnDesc" type="{}Max350Text" minOccurs="0"/>
 *         &lt;element name="AddtlData" type="{}Max20000Text" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RejectionReason2", propOrder = {
    "rjctgPtyRsn",
    "rjctnDtTm",
    "errLctn",
    "rsnDesc",
    "addtlData"
})
public class RejectionReason2 {

    @XmlElement(name = "RjctgPtyRsn", required = true)
    protected String rjctgPtyRsn;
    @XmlElement(name = "RjctnDtTm")
    protected XMLGregorianCalendar rjctnDtTm;
    @XmlElement(name = "ErrLctn")
    protected String errLctn;
    @XmlElement(name = "RsnDesc")
    protected String rsnDesc;
    @XmlElement(name = "AddtlData")
    protected String addtlData;

    /**
     * Gets the value of the rjctgPtyRsn property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRjctgPtyRsn() {
        return rjctgPtyRsn;
    }

    /**
     * Sets the value of the rjctgPtyRsn property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRjctgPtyRsn(String value) {
        this.rjctgPtyRsn = value;
    }

    /**
     * Gets the value of the rjctnDtTm property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getRjctnDtTm() {
        return rjctnDtTm;
    }

    /**
     * Sets the value of the rjctnDtTm property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setRjctnDtTm(XMLGregorianCalendar value) {
        this.rjctnDtTm = value;
    }

    /**
     * Gets the value of the errLctn property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getErrLctn() {
        return errLctn;
    }

    /**
     * Sets the value of the errLctn property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setErrLctn(String value) {
        this.errLctn = value;
    }

    /**
     * Gets the value of the rsnDesc property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRsnDesc() {
        return rsnDesc;
    }

    /**
     * Sets the value of the rsnDesc property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRsnDesc(String value) {
        this.rsnDesc = value;
    }

    /**
     * Gets the value of the addtlData property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAddtlData() {
        return addtlData;
    }

    /**
     * Sets the value of the addtlData property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAddtlData(String value) {
        this.addtlData = value;
    }

}
