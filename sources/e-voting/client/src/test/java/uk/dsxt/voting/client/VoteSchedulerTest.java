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
import uk.dsxt.voting.common.domain.nodes.AssetsHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class VoteSchedulerTest {

    @Test
    public void testLoad() throws Exception {

        Voting[] votings = new Voting[3];
        long now = System.currentTimeMillis();
        votings[0] = new Voting("0", "name0", now + 600000, now + 700000, null, "security");
        votings[1] = new Voting("1", "name1", now - 600000, now + 700000, null, "security");
        votings[2] = new Voting("2", "name2", now - 600000, now - 100000, null, "security");

        String messages="10:0,1,1,1-1-1\r\n0:1,2,2,2-2-2\r\n #\r\n2:0,3,3,3-3-3;0:1,4,4,4-4-4:-\n20:1,5,10,5-5-5\n";

        List<VoteResult> sentResults = new ArrayList<>();
        AssetsHolder client = mock(AssetsHolder.class);
        doAnswer(invocation -> {
            sentResults.add((VoteResult) invocation.getArguments()[0]);
            return "1";
        }).when(client).addClientVote(anyObject(), anyString());

        VoteScheduler scheduler = new VoteScheduler(client, messages, "001");
        Thread.sleep(100);

        assertEquals(2, sentResults.size());
        assertEquals("2", sentResults.get(0).getHolderId());
        assertEquals("4", sentResults.get(1).getHolderId());
    }
}
