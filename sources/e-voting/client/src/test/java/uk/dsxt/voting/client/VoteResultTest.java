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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class VoteResultTest {

    private static void assertBigDecimal(String expected, BigDecimal actual) throws Exception {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private static void assertBigDecimal(int expected, BigDecimal actual) throws Exception {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    @Test
    public void testSerialization() throws Exception {
        VoteResult result = new VoteResult("1,2,3 4 5,6 7 0.8");
        assertEquals("1", result.getVotingId());
        assertEquals("2", result.getHolderId());

        List<VotedAnswer> answers = new ArrayList<>(result.getAnswers());
        assertEquals(2, answers.size());
        assertEquals(3, answers.get(1).getQuestionId());
        assertEquals(4, answers.get(1).getAnswerId());
        assertBigDecimal(5, answers.get(1).getVoteAmount());
        assertEquals(6, answers.get(0).getQuestionId());
        assertEquals(7, answers.get(0).getAnswerId());
        assertBigDecimal("0.8", answers.get(0).getVoteAmount());

        assertTrue(result.equals(new VoteResult(result.toString())));

        VoteResult resultNoHolder = new VoteResult("1,,3 4 5,6 7 0.8");
        assertEquals("1", resultNoHolder.getVotingId());
        assertNull(resultNoHolder.getHolderId());
    }

    @Test
    public void testEquals() throws Exception {
        VoteResult result = new VoteResult("1,2,3 4 5,6 7 0.8");
        assertEquals(result, result);
        assertEquals(result, new VoteResult("1,2,3 4 5,6 7 0.8"));
        assertEquals(result, new VoteResult("1,2,6 7 0.8,3 4 5"));
        assertNotEquals(result, new VoteResult("1,0,3 4 5,6 7 0.8"));
        assertNotEquals(result, new VoteResult("0,1,3 4 5,6 7 0.8"));
        assertNotEquals(result, new VoteResult("1,2,3 4 5,0 7 0.8"));
        assertNotEquals(result, new VoteResult("1,2,3 4 5,6 0 0.8"));
        assertNotEquals(result, new VoteResult("1,2,3 4 5,6 7 0"));
        assertNotEquals(result, new VoteResult("1,2,3 4 5"));
        assertNotEquals(result, new VoteResult("1,2,3 4 5,6 7 0.8,0 0 0"));
        assertNotEquals(result, new VoteResult("1,,3 4 5,6 7 0.8"));
        assertNotEquals(new VoteResult("1,,3 4 5,6 7 0.8"), result);
        assertEquals(new VoteResult("1,,3 4 5,6 7 0.8"), new VoteResult("1,,3 4 5,6 7 0.8"));
    }

    @Test
    public void testSum() throws Exception {
        VoteResult result = new VoteResult("1,2,3 4 5,6 7 0.8");

        result.add(new VoteResult("1,2"));
        assertEquals(result, new VoteResult("1,2,3 4 5,6 7 0.8"));

        result.add(new VoteResult("1,2,3 4 0"));
        assertEquals(result, new VoteResult("1,2,3 4 5,6 7 0.8"));

        result.add(new VoteResult("1,2,3 4 10"));
        assertEquals(result, new VoteResult("1,2,3 4 15,6 7 0.8"));

        result.add(new VoteResult("1,2,3 4 0,6 7 10"));
        assertEquals(result, new VoteResult("1,2,3 4 15,6 7 10.8"));

        result.add(new VoteResult("1,2,3 9 0,10 7 11"));
        assertEquals(result, new VoteResult("1,2,3 4 15,6 7 10.8,3 9 0,10 7 11"));
    }

}
