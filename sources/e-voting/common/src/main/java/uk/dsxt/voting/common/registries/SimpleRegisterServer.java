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

package uk.dsxt.voting.common.registries;

import lombok.Value;
import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.domain.dataModel.Answer;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.dataModel.Question;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.utils.InternalLogicException;

import java.util.*;

@Log4j2
public class SimpleRegisterServer {

    private final Participant[] participants;

    public SimpleRegisterServer(Participant[] participants) throws InternalLogicException {
        this.participants = participants;
        validateParticipants(participants);
    }

    public Participant[] getParticipants() {
        return participants;
    }

    void validateParticipants(Participant[] participants) throws InternalLogicException {
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
    }
}
