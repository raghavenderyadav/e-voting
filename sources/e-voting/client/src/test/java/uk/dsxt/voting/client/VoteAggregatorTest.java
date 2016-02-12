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

import org.junit.Before;
import org.junit.Test;
import uk.dsxt.voting.common.datamodel.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class VoteAggregatorTest {


    private VoteAggregator aggregator;

    @Before
    public void setUp() throws Exception {
        Voting voting = new Voting("1", "name", System.currentTimeMillis(), System.currentTimeMillis()+10000, new Question[] {
                new Question(1, "q1", new Answer[]{new Answer(1, "q11"), new Answer(2, "q12")}),
                new Question(2, "q2", new Answer[]{new Answer(1, "q21")}),
        });
        Holding[] holdings = new Holding[]{
                new Holding("1", BigDecimal.ONE, null),
                new Holding("2", new BigDecimal(20), null),
                new Holding("3", BigDecimal.TEN, "2"),
                new Holding("4", BigDecimal.ONE, "2"),
        };
        BlockedPacket[] blackList = new BlockedPacket[] {new BlockedPacket("3", BigDecimal.ONE)};
        aggregator = new VoteAggregator(voting, holdings, blackList);
    }

    @Test
    public void testAggregation() throws Exception {
        assertEquals(new VoteResult("1", null), aggregator.getResult());

        // unknown holder
        aggregator.addVote(new VoteResult("1,0,1 2 1"), System.currentTimeMillis(), "0");
        assertEquals(new VoteResult("1", null), aggregator.getResult());

        //illegal sign author
        aggregator.addVote(new VoteResult("1,1,1 2 1"), System.currentTimeMillis(), "2");
        assertEquals(new VoteResult("1", null), aggregator.getResult());

        //simple vote
        aggregator.addVote(new VoteResult("1,1,1 2 1"), System.currentTimeMillis(), "1");
        assertEquals(new VoteResult("1,,1 2 1"), aggregator.getResult());

        //repeating vote
        aggregator.addVote(new VoteResult("1,1,1 2 1"), System.currentTimeMillis(), "1");
        assertEquals(new VoteResult("1,,1 2 1"), aggregator.getResult());

        //double vote
        aggregator.addVote(new VoteResult("1,1,1 2 1,2 1 1"), System.currentTimeMillis(), "1");
        assertEquals(new VoteResult("1", null), aggregator.getResult());

        //black list
        aggregator.addVote(new VoteResult("1,3,1 2 10"), System.currentTimeMillis(), "3");
        assertEquals(new VoteResult("1", null), aggregator.getResult());

        //correct
        aggregator.addVote(new VoteResult("1,2,1 2 5"), System.currentTimeMillis(), "2");
        assertEquals(new VoteResult("1,,1 2 5"), aggregator.getResult());

        //correct
        aggregator.addVote(new VoteResult("1,4,1 2 1,2 1 1"), System.currentTimeMillis(), "2");
        assertEquals(new VoteResult("1,,1 2 6,2 1 1"), aggregator.getResult());
    }

    @Test
    public void testBlackListInTree() throws Exception {
        aggregator.addVote(new VoteResult("1,2,1 2 20"), System.currentTimeMillis(), "2");
        assertEquals(new VoteResult("1", null), aggregator.getResult());
    }
}
