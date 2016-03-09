/******************************************************************************
 * e-voting system                                                            *
 * Copyright (C) 2016 DSX Technologies Limited.                               *
 * *
 * This program is free software; you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation; either version 2 of the License, or          *
 * (at your option) any later version.                                        *
 * *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied                         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * *
 * You can find copy of the GNU General Public License in LICENSE.txt file    *
 * at the top-level directory of this distribution.                           *
 * *
 * Removal or modification of this copyright notice is prohibited.            *
 * *
 ******************************************************************************/

package uk.dsxt.voting.common.iso20022;

import lombok.extern.log4j.Log4j2;
import org.joda.time.Instant;
import uk.dsxt.voting.common.datamodel.AnswerType;
import uk.dsxt.voting.common.domain.dataModel.*;
import uk.dsxt.voting.common.iso20022.jaxb.*;
import uk.dsxt.voting.common.messaging.MessagesSerializer;
import uk.dsxt.voting.common.utils.InternalLogicException;

import javax.xml.bind.*;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Log4j2
public class Iso20022Serializer implements MessagesSerializer {
    private static final String MULTI_ANSWER_TITLE = "candidate";
    private static final String SINGLE_ANSWER_TITLE = "resolution";

    @Override
    public String serialize(Voting voting) {
        try {
            //TODO add other required fields
            MeetingNotificationV04 mtgNtfctn = new MeetingNotificationV04();
            //convert questions
            List<Resolution2> resolutions = convertQuestions(voting.getQuestions());
            resolutions.stream().forEach(mtgNtfctn.getRsltn()::add);
            //TODO set XtnsnDt for cumulative questions
            //convert common info
            DateFormat2Choice voteDdln = new DateFormat2Choice();
            GregorianCalendar gcalEnd = new GregorianCalendar();
            gcalEnd.setTimeInMillis(voting.getEndTimestamp());
            gcalEnd.setTimeZone(TimeZone.getTimeZone("GMT-0"));
            XMLGregorianCalendar xmlCalEnd = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcalEnd);
            xmlCalEnd.setTimezone(Integer.MIN_VALUE);
            xmlCalEnd.setMillisecond(Integer.MIN_VALUE);
            voteDdln.setDt(xmlCalEnd);

            VoteParameters3 vote = new VoteParameters3();
            vote.setVoteDdln(voteDdln);

            MeetingNotice3 mtg = new MeetingNotice3();
            mtg.setMtgId(voting.getId());
            GregorianCalendar gcalStart = new GregorianCalendar();
            gcalStart.setTimeInMillis(voting.getBeginTimestamp());
            XMLGregorianCalendar xmlCalStart = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcalStart);
            xmlCalStart.setTimezone(Integer.MIN_VALUE);
            xmlCalStart.setTime(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
            mtg.setAnncmntDt(xmlCalStart);
            mtg.setTp(MeetingType2Code.valueOf(voting.getName().split("_")[0]));

            mtgNtfctn.setMtg(mtg);
            mtgNtfctn.setVote(vote);

            DocumentMtngNtfctn document = new DocumentMtngNtfctn();
            document.setMtgNtfctn(mtgNtfctn);

            MeetingNotification mn = new MeetingNotification();
            mn.setDocument(document);
            //convert JAXB object to string
            JAXBContext context = JAXBContext.newInstance(MeetingNotification.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            m.marshal(mn, stream);
            return stream.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Voting deserializeVoting(String message) throws InternalLogicException {
        MeetingNotification mn = null;
        try {
            JAXBContext miContext = JAXBContext.newInstance(MeetingNotification.class);
            Unmarshaller miUnmarshaller = miContext.createUnmarshaller();
            StringReader miReader = new StringReader(message);
            mn = (MeetingNotification) JAXBIntrospector.getValue(miUnmarshaller.unmarshal(miReader));
        } catch (JAXBException e) {
            throw new InternalLogicException(String.format("Couldn't deserialize message %s. Reason: %s", message, e.getMessage()));
        }

        String id = mn.getDocument().getMtgNtfctn().getMtg().getMtgId();
        LocalDate startDate = LocalDate.parse(mn.getDocument().getMtgNtfctn().getMtg().getAnncmntDt().toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        long beginTimestamp = startDate.atTime(0, 0, 0).toInstant(ZoneOffset.UTC).getEpochSecond() * 1000L;

        XMLGregorianCalendar end = mn.getDocument().getMtgNtfctn().getVote().getVoteDdln() == null
                ? mn.getDocument().getMtgNtfctn().getVote().getVoteMktDdln().getDt()
                : mn.getDocument().getMtgNtfctn().getVote().getVoteDdln().getDt();
        LocalDateTime endDate = LocalDateTime.parse(end.toString());
        long endTimestamp = endDate.toInstant(ZoneOffset.UTC).getEpochSecond() * 1000L;
        String name = String.format("%s_%s", mn.getDocument().getMtgNtfctn().getMtg().getTp(), mn.getDocument().getMtgNtfctn().getMtg().getMtgId());
        List<Question> questions = convertResolutions(mn.getDocument().getMtgNtfctn().getRsltn());
        log.debug("deserializeVoting id={} name={} begin={} end={} questionsLength={}", id, name, new Instant(beginTimestamp), new Instant(endTimestamp), questions.size());
        return new Voting(id, name, beginTimestamp, endTimestamp, questions.toArray(new Question[questions.size()]));
    }

    @Override
    public String serialize(VoteResult voteResult) throws IllegalArgumentException {
        //TODO: add other required fields
        //serialize vote results
        Vote2Choice voteChoice = new Vote2Choice();
        List<Vote4> voteInstr = voteChoice.getVoteInstr();
        for (Map.Entry<String, VotedAnswer> entry : voteResult.getAnswersByKey().entrySet()) {
            Vote4 v = new Vote4();
            v.setIssrLabl(entry.getValue().getQuestionId());
            AnswerType type = AnswerType.getType(String.valueOf(entry.getValue().getAnswerId()));
            if (type == null)
                throw new IllegalArgumentException(String.format("vote answer %s is unknown)", entry.getValue().getAnswerId()));
            switch (type) {
                case FOR: {
                    v.setFor(entry.getValue().getVoteAmount());
                    break;
                }
                case AGAINST: {
                    v.setAgnst(entry.getValue().getVoteAmount());
                    break;
                }
                case ABSTAIN: {
                    v.setAbstn(entry.getValue().getVoteAmount());
                    break;
                }
            }
            voteInstr.add(v);
        }
        VoteDetails2 voteDetails = new VoteDetails2();
        voteDetails.setVoteInstrForAgndRsltn(voteChoice);

        //serialize common info
        UnitOrFaceAmountOrCode1Choice bal = new UnitOrFaceAmountOrCode1Choice();
        bal.setUnit(voteResult.getPacketSize());
        HoldingBalance5 balance = new HoldingBalance5();
        balance.setBal(bal);
        SafekeepingAccount4 accDtl = new SafekeepingAccount4();
        accDtl.setAcctId(voteResult.getHolderId());
        accDtl.getInstdBal().add(balance);

        Instruction2 instruction = new Instruction2();
        instruction.setVoteDtls(voteDetails);
        instruction.setAcctDtls(accDtl);

        MeetingReference4 mtgRef = new MeetingReference4();
        mtgRef.setMtgId(voteResult.getVotingId());

        MeetingInstructionV04 mtgInstr = new MeetingInstructionV04();
        mtgInstr.setMtgRef(mtgRef);
        mtgInstr.getInstr().add(instruction);

        DocumentMeetingInstruction document = new DocumentMeetingInstruction();
        document.setMtgInstr(mtgInstr);

        MeetingInstruction mi = new MeetingInstruction();
        mi.setDocument(document);
        //convert JAXB object to string
        try {
            JAXBContext context = JAXBContext.newInstance(MeetingInstruction.class);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            m.marshal(mi, stream);
            return stream.toString();
        } catch (JAXBException e) {
            throw new IllegalArgumentException(String.format("unable to serialize. Reason: %s", e.getMessage()));
        }
    }

    @Override
    public VoteResult deserializeVoteResult(String message) throws InternalLogicException {
        MeetingInstruction mi = null;
        try {
            JAXBContext miContext = JAXBContext.newInstance(MeetingInstruction.class);
            Unmarshaller miUnmarshaller = miContext.createUnmarshaller();
            StringReader miReader = new StringReader(message);
            mi = (MeetingInstruction) JAXBIntrospector.getValue(miUnmarshaller.unmarshal(miReader));
        } catch (JAXBException e) {
            throw new InternalLogicException(String.format("Couldn't deserialize message %s. Reason: %s", message, e.getMessage()));
        }

        String votingId = mi.getDocument().getMtgInstr().getMtgRef().getMtgId();
        Instruction2 instruction = mi.getDocument().getMtgInstr().getInstr().get(0); // for now we suggest that there is only one instruction (without aggregation from ND)
        String holderId = instruction.getAcctDtls().getAcctId();
        BigDecimal packetSize = instruction.getAcctDtls().getInstdBal().get(0).getBal().getUnit();
        VoteResult voteResult = new VoteResult(votingId, holderId, packetSize);
        VoteDetails2 voteDetails = instruction.getVoteDtls();
        for (Vote4 vote : voteDetails.getVoteInstrForAgndRsltn().getVoteInstr()) {
            String answerId = null;
            BigDecimal amount = BigDecimal.ZERO;
            if (vote.getFor() != null) {
                answerId = AnswerType.FOR.getCode();
                amount = vote.getFor();
            } else if (vote.getAgnst() != null) {
                answerId = AnswerType.AGAINST.getCode();
                amount = vote.getAgnst();
            } else if (vote.getAbstn() != null) {
                answerId = AnswerType.ABSTAIN.getCode();
                amount = vote.getAbstn();
            }
            VotedAnswer answer = new VotedAnswer(vote.getIssrLabl(), answerId, amount);
            voteResult.getAnswersByKey().put(answer.getKey(), answer);
        }
        return voteResult;
    }

    @Override
    public VoteResult adaptVoteResultForXML(VoteResult result, Voting voting) throws InternalLogicException {
        Map<String, Question> questionsById = new HashMap<>();
        for (Question q : voting.getQuestions()) {
            questionsById.put(q.getId(), q);
            if (q.isCanSelectMultiple()) {
                for (Answer a : q.getAnswers()) {
                    questionsById.put(a.getId(), q);
                }
            }
        }
        VoteResult adaptedResult = new VoteResult(result.getVotingId(), result.getHolderId(), result.getPacketSize());
        for (VotedAnswer va : result.getAnswers()) {
            Question question = questionsById.get(va.getQuestionId());
            if (question.isCanSelectMultiple()) {
                //cumulative question
                VotedAnswer answer = new VotedAnswer(va.getAnswerId(), AnswerType.FOR.getCode(), va.getVoteAmount());
                adaptedResult.getAnswersByKey().put(answer.getKey(), answer);
            } else {
                // not cumulative question
                adaptedResult.getAnswersByKey().put(va.getKey(), va);
            }
        }
        return adaptedResult;
    }

    @Override
    public VoteResult adaptVoteResultFromXML(VoteResult result, Voting voting) throws InternalLogicException {
        Map<String, Question> questionsById = new HashMap<>();
        for (Question q : voting.getQuestions()) {
            questionsById.put(q.getId(), q);
            if (q.isCanSelectMultiple()) {
                for (Answer a : q.getAnswers()) {
                    questionsById.put(a.getId(), q);
                }
            }
        }
        VoteResult adaptedResult = new VoteResult(result.getVotingId(), result.getHolderId(), result.getPacketSize());
        for (VotedAnswer va : result.getAnswers()) {
            Question question = questionsById.get(va.getQuestionId());
            if (question != null) {
                if (question.isCanSelectMultiple()) {
                    VotedAnswer answer = new VotedAnswer(question.getId(), va.getQuestionId(), va.getVoteAmount());
                    adaptedResult.getAnswersByKey().put(answer.getKey(), answer);
                } else
                    //we found question so this is not cumulative question
                    adaptedResult.getAnswersByKey().put(va.getKey(), va);
            } else {
                //unknown question
                throw new InternalLogicException(String.format("Unknown answer %s.", va));
            }
        }
        return adaptedResult;
    }

    private List<Question> convertResolutions(List<Resolution2> resolutions) {
        //TODO: get multiplicator
        List<Question> questions = new ArrayList<>();
        String lastResolutionQuestion = null;
        String lastResolutionId = null;
        Question multiQuestion = null;
        List<Answer> multiAnswers = new ArrayList<>();
        for (Resolution2 resolution : resolutions) {
            boolean canSelectMultiple = resolution.getTitl().equals(MULTI_ANSWER_TITLE);
            if (canSelectMultiple) {
                if (multiQuestion == null) {
                    //this is first answer
                    multiQuestion = new Question(String.format("%s.multi", lastResolutionId), lastResolutionQuestion, new Answer[0]);
                }
                multiAnswers.add(new Answer(resolution.getIssrLabl(), resolution.getDesc()));
            } else {
                if (multiQuestion != null) {
                    //we need to add candidate question
                    questions.add(new Question(multiQuestion.getId(), multiQuestion.getQuestion(), multiAnswers.toArray(new Answer[multiAnswers.size()]), true, 1));
                    multiQuestion = null;
                    multiAnswers.clear();
                }
                List<Answer> answers = new ArrayList<>();
                lastResolutionQuestion = resolution.getDesc();
                lastResolutionId = resolution.getIssrLabl();
                for (VoteInstruction2Code code : resolution.getVoteInstrTp()) {
                    switch (code) {
                        case CFOR: {
                            answers.add(new Answer(AnswerType.FOR.getCode(), "За"));
                            break;
                        }
                        case CAGS: {
                            answers.add(new Answer(AnswerType.AGAINST.getCode(), "Против"));
                            break;
                        }
                        case ABST: {
                            answers.add(new Answer(AnswerType.ABSTAIN.getCode(), "Воздержался"));
                            break;
                        }
                    }
                }
                if (answers.size() > 0)
                    questions.add(new Question(resolution.getIssrLabl(), resolution.getDesc(), answers.toArray(new Answer[answers.size()])));
            }
        }
        if (multiQuestion != null)
            questions.add(new Question(multiQuestion.getId(), multiQuestion.getQuestion(), multiAnswers.toArray(new Answer[multiAnswers.size()]), true, 1));
        return questions;
    }

    private List<Resolution2> convertQuestions(Question[] questions) {
        List<Resolution2> resolutions = new ArrayList<>();
        for (Question question : questions) {
            if (question.isCanSelectMultiple()) {
                for (Answer answer : question.getAnswers()) {
                    Resolution2 resolution = new Resolution2();
                    resolution.setForInfOnly(false);
                    resolution.setIssrLabl(answer.getId());
                    resolution.setDesc(answer.getName());
                    resolution.setTitl(MULTI_ANSWER_TITLE);
                    resolution.getVoteInstrTp().add(VoteInstruction2Code.CFOR);
                    resolutions.add(resolution);
                }
            } else {
                Resolution2 resolution = new Resolution2();
                resolution.setForInfOnly(false);
                resolution.setIssrLabl(question.getId());
                resolution.setDesc(question.getQuestion());
                resolution.setTitl(SINGLE_ANSWER_TITLE);
                for (Answer answer : question.getAnswers()) {
                    AnswerType type = AnswerType.getType(answer.getId());
                    if (type != null) {
                        switch (type) {
                            case FOR: {
                                resolution.getVoteInstrTp().add(VoteInstruction2Code.CFOR);
                                break;
                            }
                            case AGAINST: {
                                resolution.getVoteInstrTp().add(VoteInstruction2Code.CAGS);
                                break;
                            }
                            case ABSTAIN: {
                                resolution.getVoteInstrTp().add(VoteInstruction2Code.ABST);
                                break;
                            }
                        }
                    }
                }
                resolutions.add(resolution);
            }
        }
        return resolutions;
    }
}
