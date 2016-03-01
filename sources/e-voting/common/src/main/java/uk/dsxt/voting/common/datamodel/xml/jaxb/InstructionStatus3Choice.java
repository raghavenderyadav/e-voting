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
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for InstructionStatus3Choice complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="InstructionStatus3Choice">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element name="GblInstrSts" type="{}InstructionStatus4Choice"/>
 *         &lt;element name="DtldInstrSts" type="{}DetailedInstructionStatus8" maxOccurs="unbounded"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InstructionStatus3Choice", propOrder = {
    "gblInstrSts",
    "dtldInstrSts"
})
public class InstructionStatus3Choice {

    @XmlElement(name = "GblInstrSts")
    protected InstructionStatus4Choice gblInstrSts;
    @XmlElement(name = "DtldInstrSts")
    protected List<DetailedInstructionStatus8> dtldInstrSts;

    /**
     * Gets the value of the gblInstrSts property.
     * 
     * @return
     *     possible object is
     *     {@link InstructionStatus4Choice }
     *     
     */
    public InstructionStatus4Choice getGblInstrSts() {
        return gblInstrSts;
    }

    /**
     * Sets the value of the gblInstrSts property.
     * 
     * @param value
     *     allowed object is
     *     {@link InstructionStatus4Choice }
     *     
     */
    public void setGblInstrSts(InstructionStatus4Choice value) {
        this.gblInstrSts = value;
    }

    /**
     * Gets the value of the dtldInstrSts property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the dtldInstrSts property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDtldInstrSts().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DetailedInstructionStatus8 }
     * 
     * 
     */
    public List<DetailedInstructionStatus8> getDtldInstrSts() {
        if (dtldInstrSts == null) {
            dtldInstrSts = new ArrayList<DetailedInstructionStatus8>();
        }
        return this.dtldInstrSts;
    }

}
