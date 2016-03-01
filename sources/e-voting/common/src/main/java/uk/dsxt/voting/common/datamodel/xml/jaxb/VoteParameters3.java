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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for VoteParameters3 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="VoteParameters3">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="SctiesQtyReqrdToVote" type="{}DecimalNumber" minOccurs="0"/>
 *         &lt;element name="PrtlVoteAllwd" type="{}YesNoIndicator"/>
 *         &lt;element name="SpltVoteAllwd" type="{}YesNoIndicator"/>
 *         &lt;element name="VoteDdln" type="{}DateFormat2Choice" minOccurs="0"/>
 *         &lt;element name="VoteSTPDdln" type="{}DateFormat2Choice" minOccurs="0"/>
 *         &lt;element name="VoteMktDdln" type="{}DateFormat2Choice" minOccurs="0"/>
 *         &lt;element name="VoteMthds" type="{}VoteMethods2" minOccurs="0"/>
 *         &lt;element name="VtngBlltElctrncAdr" type="{}CommunicationAddress4" minOccurs="0"/>
 *         &lt;element name="VtngBlltReqAdr" type="{}PostalAddress1" minOccurs="0"/>
 *         &lt;element name="RvcbltyDdln" type="{}DateFormat2Choice" minOccurs="0"/>
 *         &lt;element name="RvcbltySTPDdln" type="{}DateFormat2Choice" minOccurs="0"/>
 *         &lt;element name="RvcbltyMktDdln" type="{}DateFormat2Choice" minOccurs="0"/>
 *         &lt;element name="BnfclOwnrDsclsr" type="{}YesNoIndicator"/>
 *         &lt;element name="VoteInstrTp" type="{}VoteInstruction2Code" maxOccurs="8" minOccurs="0"/>
 *         &lt;element name="IncntivPrm" type="{}IncentivePremium3" minOccurs="0"/>
 *         &lt;element name="VoteWthPrmDdln" type="{}DateFormat2Choice" minOccurs="0"/>
 *         &lt;element name="VoteWthPrmSTPDdln" type="{}DateFormat2Choice" minOccurs="0"/>
 *         &lt;element name="VoteWthPrmMktDdln" type="{}DateFormat2Choice" minOccurs="0"/>
 *         &lt;element name="AddtlVtngRqrmnts" type="{}Max350Text" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VoteParameters3", propOrder = {
    "sctiesQtyReqrdToVote",
    "prtlVoteAllwd",
    "spltVoteAllwd",
    "voteDdln",
    "voteSTPDdln",
    "voteMktDdln",
    "voteMthds",
    "vtngBlltElctrncAdr",
    "vtngBlltReqAdr",
    "rvcbltyDdln",
    "rvcbltySTPDdln",
    "rvcbltyMktDdln",
    "bnfclOwnrDsclsr",
    "voteInstrTp",
    "incntivPrm",
    "voteWthPrmDdln",
    "voteWthPrmSTPDdln",
    "voteWthPrmMktDdln",
    "addtlVtngRqrmnts"
})
public class VoteParameters3 {

    @XmlElement(name = "SctiesQtyReqrdToVote")
    protected BigDecimal sctiesQtyReqrdToVote;
    @XmlElement(name = "PrtlVoteAllwd")
    protected boolean prtlVoteAllwd;
    @XmlElement(name = "SpltVoteAllwd")
    protected boolean spltVoteAllwd;
    @XmlElement(name = "VoteDdln")
    protected DateFormat2Choice voteDdln;
    @XmlElement(name = "VoteSTPDdln")
    protected DateFormat2Choice voteSTPDdln;
    @XmlElement(name = "VoteMktDdln")
    protected DateFormat2Choice voteMktDdln;
    @XmlElement(name = "VoteMthds")
    protected VoteMethods2 voteMthds;
    @XmlElement(name = "VtngBlltElctrncAdr")
    protected CommunicationAddress4 vtngBlltElctrncAdr;
    @XmlElement(name = "VtngBlltReqAdr")
    protected PostalAddress1 vtngBlltReqAdr;
    @XmlElement(name = "RvcbltyDdln")
    protected DateFormat2Choice rvcbltyDdln;
    @XmlElement(name = "RvcbltySTPDdln")
    protected DateFormat2Choice rvcbltySTPDdln;
    @XmlElement(name = "RvcbltyMktDdln")
    protected DateFormat2Choice rvcbltyMktDdln;
    @XmlElement(name = "BnfclOwnrDsclsr")
    protected boolean bnfclOwnrDsclsr;
    @XmlElement(name = "VoteInstrTp")
    protected List<VoteInstruction2Code> voteInstrTp;
    @XmlElement(name = "IncntivPrm")
    protected IncentivePremium3 incntivPrm;
    @XmlElement(name = "VoteWthPrmDdln")
    protected DateFormat2Choice voteWthPrmDdln;
    @XmlElement(name = "VoteWthPrmSTPDdln")
    protected DateFormat2Choice voteWthPrmSTPDdln;
    @XmlElement(name = "VoteWthPrmMktDdln")
    protected DateFormat2Choice voteWthPrmMktDdln;
    @XmlElement(name = "AddtlVtngRqrmnts")
    protected String addtlVtngRqrmnts;

    /**
     * Gets the value of the sctiesQtyReqrdToVote property.
     * 
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *     
     */
    public BigDecimal getSctiesQtyReqrdToVote() {
        return sctiesQtyReqrdToVote;
    }

    /**
     * Sets the value of the sctiesQtyReqrdToVote property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *     
     */
    public void setSctiesQtyReqrdToVote(BigDecimal value) {
        this.sctiesQtyReqrdToVote = value;
    }

    /**
     * Gets the value of the prtlVoteAllwd property.
     * 
     */
    public boolean isPrtlVoteAllwd() {
        return prtlVoteAllwd;
    }

    /**
     * Sets the value of the prtlVoteAllwd property.
     * 
     */
    public void setPrtlVoteAllwd(boolean value) {
        this.prtlVoteAllwd = value;
    }

    /**
     * Gets the value of the spltVoteAllwd property.
     * 
     */
    public boolean isSpltVoteAllwd() {
        return spltVoteAllwd;
    }

    /**
     * Sets the value of the spltVoteAllwd property.
     * 
     */
    public void setSpltVoteAllwd(boolean value) {
        this.spltVoteAllwd = value;
    }

    /**
     * Gets the value of the voteDdln property.
     * 
     * @return
     *     possible object is
     *     {@link DateFormat2Choice }
     *     
     */
    public DateFormat2Choice getVoteDdln() {
        return voteDdln;
    }

    /**
     * Sets the value of the voteDdln property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateFormat2Choice }
     *     
     */
    public void setVoteDdln(DateFormat2Choice value) {
        this.voteDdln = value;
    }

    /**
     * Gets the value of the voteSTPDdln property.
     * 
     * @return
     *     possible object is
     *     {@link DateFormat2Choice }
     *     
     */
    public DateFormat2Choice getVoteSTPDdln() {
        return voteSTPDdln;
    }

    /**
     * Sets the value of the voteSTPDdln property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateFormat2Choice }
     *     
     */
    public void setVoteSTPDdln(DateFormat2Choice value) {
        this.voteSTPDdln = value;
    }

    /**
     * Gets the value of the voteMktDdln property.
     * 
     * @return
     *     possible object is
     *     {@link DateFormat2Choice }
     *     
     */
    public DateFormat2Choice getVoteMktDdln() {
        return voteMktDdln;
    }

    /**
     * Sets the value of the voteMktDdln property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateFormat2Choice }
     *     
     */
    public void setVoteMktDdln(DateFormat2Choice value) {
        this.voteMktDdln = value;
    }

    /**
     * Gets the value of the voteMthds property.
     * 
     * @return
     *     possible object is
     *     {@link VoteMethods2 }
     *     
     */
    public VoteMethods2 getVoteMthds() {
        return voteMthds;
    }

    /**
     * Sets the value of the voteMthds property.
     * 
     * @param value
     *     allowed object is
     *     {@link VoteMethods2 }
     *     
     */
    public void setVoteMthds(VoteMethods2 value) {
        this.voteMthds = value;
    }

    /**
     * Gets the value of the vtngBlltElctrncAdr property.
     * 
     * @return
     *     possible object is
     *     {@link CommunicationAddress4 }
     *     
     */
    public CommunicationAddress4 getVtngBlltElctrncAdr() {
        return vtngBlltElctrncAdr;
    }

    /**
     * Sets the value of the vtngBlltElctrncAdr property.
     * 
     * @param value
     *     allowed object is
     *     {@link CommunicationAddress4 }
     *     
     */
    public void setVtngBlltElctrncAdr(CommunicationAddress4 value) {
        this.vtngBlltElctrncAdr = value;
    }

    /**
     * Gets the value of the vtngBlltReqAdr property.
     * 
     * @return
     *     possible object is
     *     {@link PostalAddress1 }
     *     
     */
    public PostalAddress1 getVtngBlltReqAdr() {
        return vtngBlltReqAdr;
    }

    /**
     * Sets the value of the vtngBlltReqAdr property.
     * 
     * @param value
     *     allowed object is
     *     {@link PostalAddress1 }
     *     
     */
    public void setVtngBlltReqAdr(PostalAddress1 value) {
        this.vtngBlltReqAdr = value;
    }

    /**
     * Gets the value of the rvcbltyDdln property.
     * 
     * @return
     *     possible object is
     *     {@link DateFormat2Choice }
     *     
     */
    public DateFormat2Choice getRvcbltyDdln() {
        return rvcbltyDdln;
    }

    /**
     * Sets the value of the rvcbltyDdln property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateFormat2Choice }
     *     
     */
    public void setRvcbltyDdln(DateFormat2Choice value) {
        this.rvcbltyDdln = value;
    }

    /**
     * Gets the value of the rvcbltySTPDdln property.
     * 
     * @return
     *     possible object is
     *     {@link DateFormat2Choice }
     *     
     */
    public DateFormat2Choice getRvcbltySTPDdln() {
        return rvcbltySTPDdln;
    }

    /**
     * Sets the value of the rvcbltySTPDdln property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateFormat2Choice }
     *     
     */
    public void setRvcbltySTPDdln(DateFormat2Choice value) {
        this.rvcbltySTPDdln = value;
    }

    /**
     * Gets the value of the rvcbltyMktDdln property.
     * 
     * @return
     *     possible object is
     *     {@link DateFormat2Choice }
     *     
     */
    public DateFormat2Choice getRvcbltyMktDdln() {
        return rvcbltyMktDdln;
    }

    /**
     * Sets the value of the rvcbltyMktDdln property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateFormat2Choice }
     *     
     */
    public void setRvcbltyMktDdln(DateFormat2Choice value) {
        this.rvcbltyMktDdln = value;
    }

    /**
     * Gets the value of the bnfclOwnrDsclsr property.
     * 
     */
    public boolean isBnfclOwnrDsclsr() {
        return bnfclOwnrDsclsr;
    }

    /**
     * Sets the value of the bnfclOwnrDsclsr property.
     * 
     */
    public void setBnfclOwnrDsclsr(boolean value) {
        this.bnfclOwnrDsclsr = value;
    }

    /**
     * Gets the value of the voteInstrTp property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the voteInstrTp property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVoteInstrTp().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link VoteInstruction2Code }
     * 
     * 
     */
    public List<VoteInstruction2Code> getVoteInstrTp() {
        if (voteInstrTp == null) {
            voteInstrTp = new ArrayList<VoteInstruction2Code>();
        }
        return this.voteInstrTp;
    }

    /**
     * Gets the value of the incntivPrm property.
     * 
     * @return
     *     possible object is
     *     {@link IncentivePremium3 }
     *     
     */
    public IncentivePremium3 getIncntivPrm() {
        return incntivPrm;
    }

    /**
     * Sets the value of the incntivPrm property.
     * 
     * @param value
     *     allowed object is
     *     {@link IncentivePremium3 }
     *     
     */
    public void setIncntivPrm(IncentivePremium3 value) {
        this.incntivPrm = value;
    }

    /**
     * Gets the value of the voteWthPrmDdln property.
     * 
     * @return
     *     possible object is
     *     {@link DateFormat2Choice }
     *     
     */
    public DateFormat2Choice getVoteWthPrmDdln() {
        return voteWthPrmDdln;
    }

    /**
     * Sets the value of the voteWthPrmDdln property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateFormat2Choice }
     *     
     */
    public void setVoteWthPrmDdln(DateFormat2Choice value) {
        this.voteWthPrmDdln = value;
    }

    /**
     * Gets the value of the voteWthPrmSTPDdln property.
     * 
     * @return
     *     possible object is
     *     {@link DateFormat2Choice }
     *     
     */
    public DateFormat2Choice getVoteWthPrmSTPDdln() {
        return voteWthPrmSTPDdln;
    }

    /**
     * Sets the value of the voteWthPrmSTPDdln property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateFormat2Choice }
     *     
     */
    public void setVoteWthPrmSTPDdln(DateFormat2Choice value) {
        this.voteWthPrmSTPDdln = value;
    }

    /**
     * Gets the value of the voteWthPrmMktDdln property.
     * 
     * @return
     *     possible object is
     *     {@link DateFormat2Choice }
     *     
     */
    public DateFormat2Choice getVoteWthPrmMktDdln() {
        return voteWthPrmMktDdln;
    }

    /**
     * Sets the value of the voteWthPrmMktDdln property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateFormat2Choice }
     *     
     */
    public void setVoteWthPrmMktDdln(DateFormat2Choice value) {
        this.voteWthPrmMktDdln = value;
    }

    /**
     * Gets the value of the addtlVtngRqrmnts property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAddtlVtngRqrmnts() {
        return addtlVtngRqrmnts;
    }

    /**
     * Sets the value of the addtlVtngRqrmnts property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAddtlVtngRqrmnts(String value) {
        this.addtlVtngRqrmnts = value;
    }

}
