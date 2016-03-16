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

package uk.dsxt.voting.client;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Logger;
import uk.dsxt.voting.client.datamodel.*;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.dataModel.Question;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.domain.nodes.AssetsHolder;
import uk.dsxt.voting.common.iso20022.Iso20022Serializer;
import uk.dsxt.voting.common.iso20022.jaxb.MeetingInstruction;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Log4j2
@Value
public class ClientManager {
    
    @Value
    private class SignatureInfo {
        VoteResult vote;
        String xmlToSign;
    }

    private final ObjectMapper mapper = new ObjectMapper();

    Iso20022Serializer serializer = new Iso20022Serializer();

    MeetingInstruction participantsXml;

    AssetsHolder assetsHolder;

    Logger audit;

    
    ConcurrentMap<String, SignatureInfo> signInfoByKey = new ConcurrentHashMap<>();

    CryptoHelper helper = CryptoHelper.DEFAULT_CRYPTO_HELPER;

    Map<String, Participant> participantsById;

    public ClientManager(AssetsHolder assetsHolder, MeetingInstruction participantsXml, Logger audit, Map<String, Participant> participantsById) {
        this.assetsHolder = assetsHolder;
        this.participantsXml = participantsXml;
        this.audit = audit;
        this.participantsById = participantsById;
    }

    public RequestResult getVotings(String clientId) {
        final Collection<Voting> votings = assetsHolder.getVotings();
        return new RequestResult<>(votings.stream().map(VotingWeb::new).collect(Collectors.toList()).toArray(new VotingWeb[votings.size()]), null);
    }

    public RequestResult getVoting(String votingId, String clientId) {
        final Voting voting = assetsHolder.getVoting(votingId);
        if (voting == null) {
            log.error("getVoting. Couldn't find voting with id [{}].", votingId);
            return null;
        }
        BigDecimal amount = assetsHolder.getClientPacketSize(votingId, clientId);
        long time = (long) getTime(votingId).getResult();
        return new RequestResult<>(new VotingInfoWeb(voting, amount, time), null);
    }

    public RequestResult vote(String votingId, String clientId, String votingChoice) {
        try {
            final Voting voting = assetsHolder.getVoting(votingId);
            if (voting == null) {
                log.error("vote method failed. Couldn't find voting with id [{}]", votingId);
                return new RequestResult<>(APIException.VOTING_NOT_FOUND);
            }

            BigDecimal packetSize = assetsHolder.getClientPacketSize(votingId, clientId);
            if (packetSize == null) {
                log.error("vote method failed. Client not found or can not vote. votingId [{}] clientId [{}]", votingId, clientId);
                return new RequestResult<>(APIException.CLIENT_NOT_FOUND);
            }

            VotingChoice choice = mapper.readValue(votingChoice, VotingChoice.class);
            log.debug("Vote for voting [{}] from client [{}] received.", votingId, clientId);

            VoteResult result = new VoteResult(votingId, clientId, packetSize);
            for (Map.Entry<String, QuestionChoice> entry : choice.getQuestionChoices().entrySet()) {
                Optional<Question> question = Arrays.stream(voting.getQuestions()).filter(q -> q.getId().equals(entry.getKey())).findAny();
                if (!question.isPresent()) {
                    log.error("vote method failed. Couldn't find question with id={} in votingId={}.", entry.getKey(), votingId);
                    return new RequestResult<>(APIException.UNKNOWN_EXCEPTION);
                }
                for (Map.Entry<String, BigDecimal> answer : entry.getValue().getAnswerChoices().entrySet()) {
                    if (answer.getValue().signum() == 0)
                        continue;
                    result.setAnswer(question.get().getId(), answer.getKey(), answer.getValue());
                }
            }
            //generate xml body and get vote results
            VotingInfoWeb infoWeb = getVotingResults(result, voting);
            VoteResult adaptedResult = serializer.adaptVoteResultForXML(result, voting);
            String xmlBody = serializer.serialize(adaptedResult);
            //TODO: send xml string without header (and handle correctly send time)
            signInfoByKey.put(generateKeyForDocument(clientId, votingId), new SignatureInfo(result, xmlBody));
            infoWeb.setXmlBody(xmlBody);
            return new RequestResult<>(infoWeb, null);
        } catch (JsonMappingException je) {
            log.error("vote method failed. Couldn't parse votingChoice JSON. votingId: {}, votingChoice: {}", votingId, votingChoice, je.getMessage());
            return new RequestResult<>(APIException.UNKNOWN_EXCEPTION);
        } catch (Exception e) {
            log.error("vote method failed. Couldn't process votingChoice. votingId: {}, votingChoice: {}", votingId, votingChoice, e);
            return new RequestResult<>(APIException.UNKNOWN_EXCEPTION);
        }
    }

    public RequestResult signVote(String votingId, String clientId, String signature) {
        final Voting voting = assetsHolder.getVoting(votingId);
        if (voting == null) {
            log.debug("signVote. Voting with id={} not found.", votingId);
            return new RequestResult<>(APIException.VOTING_NOT_FOUND);
        }
        SignatureInfo info =  signInfoByKey.get(generateKeyForDocument(clientId, votingId));
        if (info == null) {
            log.error("signVote failed. Client {} doesn't have vote for voting {}", clientId, votingId);
            return new RequestResult<>(APIException.UNKNOWN_EXCEPTION);
        }
        String documentString = info.getXmlToSign();
        try {
            boolean result = helper.verifySignature(documentString, signature, helper.loadPublicKey(participantsById.get(clientId).getPublicKey()));
            if (!result) {
                log.error("signVote failed. Incorrect signature {} for client {} and voting {}", signature, clientId, votingId);
                return new RequestResult<>(APIException.INVALID_SIGNATURE);
            }
        } catch (Exception e) {
            log.error("signVote failed. Failed to check signature {} for client {} and voting {}", signature, clientId, votingId, e.getMessage());
            return new RequestResult<>(APIException.INVALID_SIGNATURE);
        }
        audit.info("signVote. Client {} successfully signed voting {}", clientId, votingId);
        assetsHolder.addClientVote(info.getVote());
        return new RequestResult<>(true, null);
    }

    private String generateKeyForDocument(String clientId, String votingId) {
        return String.format("%s_%s", clientId, votingId);
    }

    public RequestResult votingResults(String votingId, String clientId) {
        final Voting voting = assetsHolder.getVoting(votingId);
        if (voting == null) {
            log.debug("votingResults. Voting with id={} not found.", votingId);
            return new RequestResult<>(APIException.VOTING_NOT_FOUND);
        }
        final VoteResult clientVote = assetsHolder.getClientVote(votingId, clientId);
        if (clientVote == null) {
            log.debug("votingResults. Client vote result with id={} for client with id={} not found.", votingId, clientId);
            return new RequestResult<>(APIException.VOTE_NOT_FOUND);
        }
        return new RequestResult<>(getVotingResults(clientVote, voting), null);
    }

    private VotingInfoWeb getVotingResults(VoteResult clientVote, Voting voting) {
        List<QuestionWeb> results = new ArrayList<>();
        for (Question question : voting.getQuestions()) {
            results.add(new QuestionWeb(question, clientVote, false));
        }
        return new VotingInfoWeb(results.toArray(new QuestionWeb[results.size()]), assetsHolder.getClientPacketSize(voting.getId(), clientVote.getHolderId()), 0, null);
    }

    public RequestResult getTime(String votingId) {
        final Voting voting = assetsHolder.getVoting(votingId);
        if (voting == null) {
            log.debug("votingResults. Voting with id={} not found.", votingId);
            return new RequestResult<>(APIException.VOTING_NOT_FOUND);
        }
        long now = System.currentTimeMillis();
        if (now < voting.getBeginTimestamp() || now > voting.getEndTimestamp())
            return new RequestResult<>(-1, null);
        return new RequestResult<>(voting.getEndTimestamp() - now, null);
    }

    public RequestResult getConfirmedClientVotes(String votingId) {
        List<VoteResultWeb> results = new ArrayList<>();
        final Collection<VoteResult> votes = assetsHolder.getConfirmedClientVotes(votingId);
        results.addAll(votes.stream().map(VoteResultWeb::new).collect(Collectors.toList()));
        return new RequestResult<>(results.toArray(new VoteResultWeb[results.size()]), null);
    }

    public RequestResult getAllClientVotes(String votingId) {
        List<VoteResultWeb> results = new ArrayList<>();
        final Collection<VoteResult> votes = assetsHolder.getAllClientVotes(votingId);
        results.addAll(votes.stream().map(VoteResultWeb::new).collect(Collectors.toList()));
        return new RequestResult<>(results.toArray(new VoteResultWeb[results.size()]), null);
    }

    public RequestResult votingTotalResults(String votingId) {
        final Voting voting = assetsHolder.getVoting(votingId);
        if (voting == null) {
            log.debug("votingTotalResults. Voting with id={} not found.", votingId);
            return new RequestResult<>(APIException.VOTING_NOT_FOUND);
        }
        final VoteResult voteResults = assetsHolder.getTotalVotingResult(votingId);
        if (voteResults == null) {
            log.debug("votingTotalResults. Total results for voting with id={} not found.", votingId);
            return new RequestResult<>(APIException.VOTE_RESULTS_NOT_FOUND);
        }

        List<QuestionWeb> results = new ArrayList<>();
        for (Question question : voting.getQuestions()) {
            results.add(new QuestionWeb(question, voteResults, true));
        }
        return new RequestResult<>(new VotingInfoWeb(results.toArray(new QuestionWeb[results.size()]), BigDecimal.ZERO, 0, null), null);
    }


}
