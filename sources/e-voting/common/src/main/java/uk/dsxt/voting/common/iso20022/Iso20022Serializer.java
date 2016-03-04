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

import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.messaging.MessagesSerializer;
import uk.dsxt.voting.common.utils.InternalLogicException;

public class ISO20022Serializer implements MessagesSerializer {

    @Override
    public String serialize(Voting voting) {
        //TODO
        return String.format("%s\t%s\t%d\t%d\t", voting.getId(), voting.getName(), voting.getBeginTimestamp(), voting.getEndTimestamp());
    }

    @Override
    public Voting deserializeVoting(String message) throws InternalLogicException {
        //TODO
        String[] terms = message.split("\t");
        return new Voting(terms[0], terms[1], Long.parseLong(terms[2]), Long.parseLong(terms[3]), null);
    }

    @Override
    public String serialize(VoteResult voteResult) {
        //TODO
        return voteResult.toString();
    }

    @Override
    public VoteResult deserializeVoteResult(String message) throws InternalLogicException {
        //TODO
        return new VoteResult(message);
    }
}
