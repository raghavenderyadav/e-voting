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

import lombok.AllArgsConstructor;
import lombok.Value;
import uk.dsxt.voting.common.datamodel.BlackListEntry;
import uk.dsxt.voting.common.datamodel.Voter;
import uk.dsxt.voting.common.datamodel.Voting;
import uk.dsxt.voting.common.datamodel.VotingRight;

@Value
@AllArgsConstructor
public class RegistriesServerManager {

    Voter[] voters;
    VotingRight[] votingRights;
    Voting voting;
    BlackListEntry[] blackList;

    public VotingRight[] getVotingRights() {
        return votingRights;
    }

    public Voter[] getVoters() {
        return voters;
    }

    public Voting getVoting() {
        return voting;
    }

    public BlackListEntry[] getBlackList() {
        return blackList;
    }
}
