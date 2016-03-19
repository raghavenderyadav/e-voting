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

package uk.dsxt.voting.common.domain.nodes;

import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.VoteStatus;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.utils.InternalLogicException;

import java.math.BigDecimal;
import java.util.Collection;

public interface AssetsHolder extends VoteAcceptor {
    
    String EMPTY_SIGNATURE = "-";

    Voting getVoting(String votingId);

    Collection<Voting> getVotings();

    VoteResult getTotalVotingResult(String votingId);

    Collection<VoteResult> getAllClientVotes(String votingId);

    Collection<VoteStatus> getConfirmedClientVotes(String votingId);

    VoteResult getClientVote(String votingId, String clientId);

    String addClientVote(VoteResult result, String signature) throws InternalLogicException;

    BigDecimal getClientPacketSize(String votingId, String clientId);
}
