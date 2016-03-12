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

package uk.dsxt.voting.client.web;

import uk.dsxt.voting.client.datamodel.RequestResult;

public interface VotingAPI {

    RequestResult login(String login, String password);

    RequestResult logout(String cookie);

    RequestResult getVotings(String cookie);

    RequestResult getVoting(String cookie, String votingId);

    RequestResult vote(String cookie, String votingId, String votingChoice);

    RequestResult votingResults(String cookie, String votingId);

    RequestResult votingTotalResults(String cookie, String votingId);

    RequestResult getTime(String cookie, String votingId);

    RequestResult getConfirmedClientVotes(String cookie, String votingId);

    RequestResult getAllClientVotes(String cookie, String votingId);
}
