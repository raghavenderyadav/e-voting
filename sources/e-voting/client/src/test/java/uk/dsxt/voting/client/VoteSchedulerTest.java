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

import org.junit.Test;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.demo.ResultsBuilder;
import uk.dsxt.voting.common.networking.VoteAggregation;
import uk.dsxt.voting.common.networking.VotingClient;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class VoteSchedulerTest {

    @Test
    public void testLoad() throws Exception {

        Voting[] votings = new Voting[3];
        long now = System.currentTimeMillis();
        votings[0] = new Voting("0", "name0", now + 600000, now + 700000, null);
        votings[1] = new Voting("1", "name1", now - 600000, now + 700000, null);
        votings[2] = new Voting("2", "name2", now - 600000, now - 100000, null);

        String messages="0:0,1,1-1-1\r\n10:1,2,2-2-2\r\n #\r\n2:0,3,3-3-3;01:1,4,4-4-4:-\n20:1,5,5-5-5\n";

        List<VoteResult> sentResults = new ArrayList<>();
        VotingClient client = mock(VotingClient.class);
        doAnswer(invocation -> {
            sentResults.add((VoteResult) invocation.getArguments()[0]);
            return true;
        }).when(client).sendVoteResult(anyObject());

        List<String> sentToBuilderResults = new ArrayList<>();
        List<String> sentAggregatedResults = new ArrayList<>();
        ResultsBuilder builder = mock(ResultsBuilder.class);
        doAnswer(invocation -> {
            sentToBuilderResults.add((String) invocation.getArguments()[0]);
            return null;
        }).when(builder).addVote(anyString());
        doAnswer(invocation -> {
            sentAggregatedResults.add((String) invocation.getArguments()[1]);
            return null;
        }).when(builder).addResult(anyString(), anyString());

        VoteAggregation aggregation = mock(VoteAggregation.class);
        when(aggregation.getResult("2")).thenReturn(new VoteResult("22", null));

        VoteScheduler scheduler = new VoteScheduler(client, builder, aggregation, votings, messages, "001");
        scheduler.run();
        Thread.sleep(100);

        assertEquals(2, sentResults.size());
        assertEquals("4", sentResults.get(0).getHolderId());
        assertEquals("2", sentResults.get(1).getHolderId());
        assertEquals(1, sentToBuilderResults.size());
        assertEquals("2", new VoteResult(sentToBuilderResults.get(0)).getHolderId());
        assertEquals(1, sentAggregatedResults.size());
        assertEquals(null, new VoteResult(sentAggregatedResults.get(0)).getHolderId());
        assertEquals("22", new VoteResult(sentAggregatedResults.get(0)).getVotingId());
    }
}
