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

package uk.dsxt.voting.common;

import org.junit.Test;
import uk.dsxt.voting.common.networking.MessageContent;
import uk.dsxt.voting.common.utils.CryptoHelper;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class MessageContentTest {

    @Test
    public void testValidMessage() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(CryptoHelper.ALGORITHM);
        KeyPair pair = gen.generateKeyPair();
        String originalText = "xxx";
        String ownerId = "007";

        Map<String, String> fields = new HashMap<>();
        fields.put("TTT", originalText);
        long minTime = System.currentTimeMillis();
        byte[] body = MessageContent.buildOutputMessage(MessageContent.TYPE_VOTE_RESULT, ownerId, pair.getPrivate(), fields);
        long maxTime = System.currentTimeMillis();

        MessageContent content = new MessageContent(body);

        assertEquals(MessageContent.TYPE_VOTE_RESULT, content.getType());
        assertEquals(ownerId, content.getAuthor());
        assertEquals(originalText, content.getField("TTT"));
        assertTrue(minTime <= content.getFieldTimestamp());
        assertTrue(maxTime >= content.getFieldTimestamp());
        assertTrue(content.checkSign(pair.getPublic()));
    }

    @Test
    public void testEscapedSymbols() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(CryptoHelper.ALGORITHM);
        KeyPair pair = gen.generateKeyPair();
        String originalText = " abc=x;y!o!!p";

        Map<String, String> fields = new HashMap<>();
        fields.put("TTT", originalText);
        byte[] body = MessageContent.buildOutputMessage(MessageContent.TYPE_VOTE_RESULT, "007a", pair.getPrivate(), fields);

        MessageContent content = new MessageContent(body);

        assertEquals(MessageContent.TYPE_VOTE_RESULT, content.getType());
        assertEquals(originalText, content.getField("TTT"));
    }

    @Test
    public void testWrongSignature() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance(CryptoHelper.ALGORITHM);
        KeyPair pair = gen.generateKeyPair();

        byte[] body = MessageContent.buildOutputMessage(MessageContent.TYPE_VOTE_RESULT, "007a", pair.getPrivate(), null);

        MessageContent content = new MessageContent(body);

        assertFalse(content.checkSign(gen.generateKeyPair().getPublic()));
    }
}
