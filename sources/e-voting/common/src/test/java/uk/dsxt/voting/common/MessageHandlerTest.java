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

import org.junit.Ignore;
import org.junit.Test;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.messaging.Message;
import uk.dsxt.voting.common.messaging.MessageContent;
import uk.dsxt.voting.common.networking.MessageHandler;
import uk.dsxt.voting.common.networking.WalletManager;
import uk.dsxt.voting.common.utils.crypto.CryptoHelperImpl;
import uk.dsxt.voting.common.utils.crypto.KeyPair;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageHandlerTest {

    private final CryptoHelperImpl cryptoHelper = CryptoHelperImpl.DEFAULT_CRYPTO_HELPER;

    @Test
    @Ignore
    public void testMessageFilter() throws Exception {
        KeyPair[] keys = cryptoHelper.createCryptoKeysGenerator().generateKeys(3);

        Participant[] participants = new Participant[3];
        participants[0] = new Participant("00", "name00", keys[0].getPublicKey());
        participants[1] = new Participant("01", "name01", keys[1].getPublicKey());
        participants[2] = new Participant("02", "name02", keys[2].getPublicKey());
        Map<String, PublicKey> participantKeysById = new HashMap<>();
        for(Participant participant : participants) {
            if (participant.getPublicKey() == null || participant.getPublicKey().isEmpty())
                continue;
            PublicKey key = cryptoHelper.loadPublicKey(participant.getPublicKey());
            participantKeysById.put(participant.getId(), key);
        }

        WalletManager walletManager = mock(WalletManager.class);
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("m0", MessageContent.buildOutputMessage("X0", "10", cryptoHelper.loadPrivateKey(keys[0].getPrivateKey()), cryptoHelper, null), true));
        messages.add(new Message("m1", MessageContent.buildOutputMessage("X1", "00", cryptoHelper.loadPrivateKey(keys[0].getPrivateKey()), cryptoHelper, null), true));
        messages.add(new Message("m2", MessageContent.buildOutputMessage("X2", "01", cryptoHelper.loadPrivateKey(keys[0].getPrivateKey()), cryptoHelper, null), true));
        messages.add(new Message("m3", MessageContent.buildOutputMessage("X3", "00", cryptoHelper.loadPrivateKey(keys[0].getPrivateKey()), cryptoHelper, null), true));
        messages.add(new Message("m1", MessageContent.buildOutputMessage("X1", "00", cryptoHelper.loadPrivateKey(keys[0].getPrivateKey()), cryptoHelper, null), true));
        when(walletManager.getNewMessages(0)).thenReturn(messages);

        List<MessageContent> filteredContents = new ArrayList<>();
        List<String> filteredIds = new ArrayList<>();

        MessageHandler handler = new MessageHandler(walletManager, CryptoHelperImpl.DEFAULT_CRYPTO_HELPER, participantKeysById) {
            @Override
            protected void handleNewMessage(MessageContent messageContent, String messageId, boolean isCommitted, String authorId) {
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
