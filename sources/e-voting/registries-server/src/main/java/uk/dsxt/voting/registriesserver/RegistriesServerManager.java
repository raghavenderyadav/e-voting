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

package uk.dsxt.voting.registriesserver;

import lombok.Value;
import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.datamodel.*;

import java.math.BigDecimal;
import java.util.*;

@Log4j2
@Value
public class RegistriesServerManager {

    Participant[] participants;
    Holding[] holdings;
    Voting[] votings;
    BlockedPacket[] blackList;

    public RegistriesServerManager(Participant[] participants, Holding[] holdings,
                                   Voting[] votings, BlockedPacket[] blackList) throws InternalLogicException {
        validateData(participants, holdings, votings, blackList);

        this.participants = participants;
        this.holdings = holdings;
        this.votings = votings;
        this.blackList = blackList;
    }

    public Holding[] getHoldings() {
        return holdings;
    }

    public Participant[] getParticipants() {
        return participants;
    }

    public Voting[] getVotings() {
        return votings;
    }

    public BlockedPacket[] getBlackList() {
        return blackList;
    }

    private void validateData(Participant[] participants, Holding[] holdings,
                              Voting[] votings, BlockedPacket[] blackList) throws InternalLogicException {
        validateVotings(votings);

        Map<String, Participant> participantsById = validateAndMapParticipants(participants);

        Map<String, BigDecimal> holdersPacketSizeByHolderId = validateAndMapHoldings(holdings, participantsById);

        validateBlackList(blackList, participantsById, holdersPacketSizeByHolderId);
    }

    void validateVotings(Voting[] votings) throws InternalLogicException {
        if (votings == null || votings.length == 0)
            throw new InternalLogicException("validateVotings failed. votings are null or empty.");
        for (int i = 0; i < votings.length; i++) {
            Voting v = votings[i];
            if (v == null || v.getId() == null || v.getId().isEmpty() || v.getName() == null || v.getName().isEmpty() || v.getQuestions() == null || v.getQuestions().length == 0)
                throw new InternalLogicException(String.format("validateVotings failed. One of the voting fields are incorrect. index=%d", i));

            for (int j = 0; j < v.getQuestions().length; j++) {
                Question q = v.getQuestions()[j];
                if (q == null || q.getId() <= 0 || q.getQuestion() == null || q.getAnswers() == null || q.getAnswers().length == 0)
                    throw new InternalLogicException(String.format("validateVotings failed. One of the question fields are incorrect. voting index=%d, question index=%d", i, j));

                for (int k = 0; k < q.getAnswers().length; k++) {
                    Answer a = q.getAnswers()[k];
                    if (a == null || a.getId() <= 0 || a.getName() == null)
                        throw new InternalLogicException(String.format("validateVotings failed. One of the answer fields. voting index=%d, question index=%d, answer index=%d", i, j, k));
                }
            }
        }
    }

    Map<String, Participant> validateAndMapParticipants(Participant[] participants) throws InternalLogicException {
        Map<String, Participant> participantsById = new HashMap<>();
        //participant validation
        if (participants == null || participants.length == 0)
            throw new InternalLogicException("validateAndMapParticipants failed. participants are null or empty.");
        for (int i = 0; i < participants.length; i++) {
            Participant p = participants[i];
            if (p == null || p.getId() == null || p.getId().isEmpty() || p.getName() == null || p.getName().isEmpty() || p.getPublicKey() == null || p.getPublicKey().isEmpty())
                throw new InternalLogicException(String.format("validateAndMapParticipants failed. One of the participant fields are incorrect. index=%d", i));
            if (participantsById.containsKey(p.getId()))
                throw new InternalLogicException(String.format("validateAndMapParticipants failed. Participant with id %s is represented twice.", p.getId()));
            participantsById.put(p.getId(), p);
        }
        return participantsById;
    }

    private Map<String, BigDecimal> validateAndMapHoldings(Holding[] holdings, Map<String, Participant> participantsById) throws InternalLogicException {
        Map<String, BigDecimal> holdersPacketSizeByHolderId = new HashMap<>();
        Map<String, String> nominalHoldersByHolderId = new HashMap<>();
        if (holdings == null || holdings.length == 0)
            throw new InternalLogicException("validateAndMapHoldings failed. holdings are null or empty.");
        for (int i = 0; i < holdings.length; i++) {
            Holding h = holdings[i];
            if (h.getHolderId() == null || h.getPacketSize() == null || h.getPacketSize().compareTo(BigDecimal.ZERO) <= 0)
                throw new InternalLogicException(String.format("validateAndMapHoldings failed. One of the holding fields are incorrect. index=%d", i));
            if (holdersPacketSizeByHolderId.containsKey(h.getHolderId()))
                throw new InternalLogicException(String.format("validateAndMapHoldings failed. Holding with holder id %s is represented twice.", h.getHolderId()));
            if (!participantsById.containsKey(h.getHolderId()))
                throw new InternalLogicException(String.format("validateAndMapHoldings failed. Holding's holder id %s is unknown. index=%d", h.getHolderId(), i));
            //check nominal holder
            if (h.getNominalHolderId() != null) {
                if (!participantsById.containsKey(h.getNominalHolderId()))
                    throw new InternalLogicException(String.format("validateAndMapHoldings failed. Holding's nominal holder id %s is unknown. index=%d", h.getNominalHolderId(), i));
                nominalHoldersByHolderId.put(h.getHolderId(), h.getNominalHolderId());
            }
            //check for trees are not cyclic
            for (Map.Entry<String, String> entry : nominalHoldersByHolderId.entrySet()) {
                List<String> handledNodes = new ArrayList<>();
                handledNodes.add(entry.getKey());
                validateTreeAcyclic(entry.getValue(), nominalHoldersByHolderId, handledNodes);
            }

            holdersPacketSizeByHolderId.put(h.getHolderId(), h.getPacketSize());
        }

        if (participantsById.size() != holdersPacketSizeByHolderId.size())
            throw new InternalLogicException(String.format("validateAndMapHoldings failed. Participants size %d doesn't equal to holdings size %d",
                    participantsById.size(), holdersPacketSizeByHolderId.size()));

        return holdersPacketSizeByHolderId;
    }

    private void validateTreeAcyclic(String parent, Map<String, String> pairs, List<String> handledNodes) throws InternalLogicException {
        if (handledNodes.contains(parent))
            throw new InternalLogicException(String.format("validateTreeAcyclic failed. Cyclic tree found. nodes %s", Arrays.toString(handledNodes.toArray())));
        if (pairs.containsKey(parent)) {
            handledNodes.add(parent);
            validateTreeAcyclic(pairs.get(parent), pairs, handledNodes);
        }
    }

    private void validateBlackList(BlockedPacket[] blackList, Map<String, Participant> participantsById,
                                   Map<String, BigDecimal> holdersPacketSizeByHolderId) throws InternalLogicException {
        if (blackList == null)
            throw new InternalLogicException("validateBlackList failed. blackList is null.");
        for (int i = 0; i < blackList.length; i++) {
            BlockedPacket bp = blackList[i];
            if (bp.getHolderId() == null || bp.getPacketSize() == null || bp.getPacketSize().compareTo(BigDecimal.ZERO) <= 0)
                throw new InternalLogicException(String.format("validateBlackList failed. One of the blacklist entry fields are incorrect. index=%d", i));
            if (!participantsById.containsKey(bp.getHolderId()))
                throw new InternalLogicException(String.format("validateBlackList failed. There is no participant with id %s. index=%d", bp.getHolderId(), i));
            //check that blocked packet size value is correct
            BigDecimal originalPacketSize = holdersPacketSizeByHolderId.get(bp.getHolderId());
            if (originalPacketSize.compareTo(bp.getPacketSize()) < 0)
                throw new InternalLogicException(String.format("validateBlackList failed. Blocked packet size %s is greater than original %s for id %s. index=%d",
                        originalPacketSize, bp.getPacketSize(), bp.getHolderId(), i));
        }
    }

    public void stop() {
        try {
            log.warn("stop called.");
            RegistriesServerMain.shutdown();
        } catch (Exception e) {
            log.error("stop failed. unable to stop module.", e);
        }
    }
}
