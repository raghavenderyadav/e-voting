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
import uk.dsxt.voting.common.messaging.MessageContent;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;
import uk.dsxt.voting.common.utils.crypto.CryptoKeysGenerator;
import uk.dsxt.voting.common.utils.crypto.KeyPair;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MessageContentTest {

    private CryptoHelper cryptoHelper = CryptoHelper.DEFAULT_CRYPTO_HELPER;

    @Test
    public void testValidMessage() throws Exception {
        CryptoKeysGenerator gen = cryptoHelper.createCryptoKeysGenerator();
        KeyPair pair = gen.generateKeyPair();
        String originalText = "xxx";
        String ownerId = "007";

        Map<String, String> fields = new HashMap<>();
        fields.put("TTT", originalText);
        long minTime = System.currentTimeMillis();
        byte[] body = MessageContent.buildOutputMessage("MSG_TYPE", ownerId, cryptoHelper.loadPrivateKey(pair.getPrivateKey()), cryptoHelper, fields);
        long maxTime = System.currentTimeMillis();

        MessageContent content = new MessageContent(body);

        assertEquals("MSG_TYPE", content.getType());
        assertEquals(ownerId, content.getAuthor());
        assertEquals(originalText, content.getField("TTT"));
        assertTrue(minTime <= content.getFieldTimestamp());
        assertTrue(maxTime >= content.getFieldTimestamp());
        assertTrue(content.checkSign(cryptoHelper.loadPublicKey(pair.getPublicKey()), cryptoHelper));
    }

    @Test
    public void testEscapedSymbols() throws Exception {
        CryptoKeysGenerator gen = cryptoHelper.createCryptoKeysGenerator();
        KeyPair pair = gen.generateKeyPair();
        String originalText = " abc=x;y!o!!p";

        Map<String, String> fields = new HashMap<>();
        fields.put("TTT", originalText);
        byte[] body = MessageContent.buildOutputMessage("MSG_TYPE", "007a", cryptoHelper.loadPrivateKey(pair.getPrivateKey()), cryptoHelper, fields);

        MessageContent content = new MessageContent(body);

        assertEquals("MSG_TYPE", content.getType());
        assertEquals(originalText, content.getField("TTT"));
    }

    @Test
    public void testWrongSignature() throws Exception {
        CryptoKeysGenerator gen = cryptoHelper.createCryptoKeysGenerator();
        KeyPair pair = gen.generateKeyPair();

        byte[] body = MessageContent.buildOutputMessage("MSG_TYPE", "007a", cryptoHelper.loadPrivateKey(pair.getPrivateKey()), cryptoHelper, null);

        MessageContent content = new MessageContent(body);

        assertFalse(content.checkSign(cryptoHelper.loadPublicKey(pair.getPublicKey()), cryptoHelper));
    }
}
