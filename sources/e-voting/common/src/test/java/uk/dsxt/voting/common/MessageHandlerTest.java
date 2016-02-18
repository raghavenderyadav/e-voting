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
import uk.dsxt.voting.common.datamodel.KeyPair;
import uk.dsxt.voting.common.datamodel.Participant;
import uk.dsxt.voting.common.networking.*;
import uk.dsxt.voting.common.utils.CryptoHelper;
import uk.dsxt.voting.common.utils.CryptoKeysGenerator;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

public class MessageHandlerTest {

    @Test
    public void testMessageFilter() throws Exception {
        KeyPair[] keys = CryptoKeysGenerator.generateKeys(3);

        Participant[] participants = new Participant[3];
        participants[0] = new Participant("00", "name00", keys[0].getPublicKey());
        participants[1] = new Participant("01", "name01", keys[1].getPublicKey());
        participants[2] = new Participant("02", "name02", keys[2].getPublicKey());

        WalletManager walletManager = mock(WalletManager.class);
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("m0", MessageContent.buildOutputMessage("X0", "10", CryptoHelper.loadPrivateKey(keys[0].getPrivateKey()), null)));
        messages.add(new Message("m1", MessageContent.buildOutputMessage("X1", "00", CryptoHelper.loadPrivateKey(keys[0].getPrivateKey()), null)));
        messages.add(new Message("m2", MessageContent.buildOutputMessage("X2", "01", CryptoHelper.loadPrivateKey(keys[0].getPrivateKey()), null)));
        messages.add(new Message("m3", MessageContent.buildOutputMessage("X3", "00", CryptoHelper.loadPrivateKey(keys[0].getPrivateKey()), null)));
        messages.add(new Message("m1", MessageContent.buildOutputMessage("X1", "00", CryptoHelper.loadPrivateKey(keys[0].getPrivateKey()), null)));
        when(walletManager.getNewMessages(0)).thenReturn(messages);

        List<MessageContent> filteredContents = new ArrayList<>();
        List<String> filteredIds = new ArrayList<>();

        MessageHandler handler = new MessageHandler(walletManager, participants) {
            @Override
            protected void handleNewMessage(MessageContent messageContent, String messageId) {
                filteredContents.add(messageContent);
                filteredIds.add(messageId);
            }
        };
        handler.run(1000000000L);
        Thread.sleep(100);

        assertEquals(2, filteredContents.size());
        assertEquals("m1", filteredIds.get(0));
        assertEquals("X1", filteredContents.get(0).getType());
        assertEquals("m3", filteredIds.get(1));
        assertEquals("X3", filteredContents.get(1).getType());
    }
}
