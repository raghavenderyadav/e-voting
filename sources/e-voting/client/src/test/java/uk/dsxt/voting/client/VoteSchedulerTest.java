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
import uk.dsxt.voting.common.datamodel.Voting;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class VoteSchedulerTest {

    @Test
    public void testLoad() throws Exception {

        Voting[] votings = new Voting[2];
        long now = System.currentTimeMillis();
        votings[0] = new Voting("0", "name0", now + 600000, now + 700000, null);
        votings[1] = new Voting("1", "name1", now-600000, now + 700000, null);

        String messages="0:0,1,1 1 1\r\n10:1,2,2 2 2\r\n #\r\n2:0,3,3 3 3\r\n01:1,4,4 4 4\n20:1,5,5 5 5\n";

        List<VoteResult> sentResults = new ArrayList<>();
        VoitingClient client = mock(VoitingClient.class);
        doAnswer(invocation -> {
            sentResults.add((VoteResult) invocation.getArguments()[0]);
            return null;
        }).when(client).sendVoteResult(anyObject());

        VoteScheduler scheduler = new VoteScheduler(client, votings, messages);
        scheduler.run();
        Thread.sleep(100);
        assertEquals(2, sentResults.size());
        assertEquals("4", sentResults.get(0).getHolderId());
        assertEquals("2", sentResults.get(1).getHolderId());
    }
}
