/******************************************************************************
 * e-voting system                                                            *
 * Copyright (C) 2016 DSX Technologies Limited.                               *
 *                                                                            *
 * This program is free software; you can redistribute it and/or modify       *
 * it under the terms of the GNU General Public License as published by       *
 * the Free Software Foundation; either version 2 of the License, or          *
 * (at your option) any later version.                                        *
 *                                                                            *
 * This program is distributed in the hope that it will be useful,            *
 * but WITHOUT ANY WARRANTY; without even the implied                         *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 *                                                                            *
 * You can find copy of the GNU General Public License in LICENSE.txt file    *
 * at the top-level directory of this distribution.                           *
 *                                                                            *
 * Removal or modification of this copyright notice is prohibited.            *
 *                                                                            *
 ******************************************************************************/

package uk.dsxt.voting.common.networking;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.datamodel.Participant;
import uk.dsxt.voting.common.utils.CryptoHelper;

import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.*;

@Log4j2
public abstract class MessageHandler {

    protected final WalletManager walletManager;

    private final Map<String, PublicKey> publicKeysById = new HashMap<>();

    private long lastNewMessagesRequestTime;

    protected MessageHandler(WalletManager walletManager, Participant[] participants) {
        this.walletManager = walletManager;
        for(Participant participant : participants) {
            if (participant.getPublicKey() != null) {
                try {
                    PublicKey key = CryptoHelper.loadPublicKey(participant.getPublicKey());
                    publicKeysById.put(participant.getId(), key);
                } catch (GeneralSecurityException e) {
                    log.error("Can not extract public key for participant {}({})", participant.getName(), participant.getId());
                }
            }
        }

        walletManager.runWallet();
    }

    public void run(long newMessagesRequestInterval) {
        Thread messagesHandler = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(newMessagesRequestInterval);
                } catch (InterruptedException ignored) {
                    return;
                }
                checkNewMessages();
            }
        }, "messagesHandler");
        messagesHandler.start();
        log.info("VoitingClient runs");
    }

    private void checkNewMessages() {
        List<Message> newMessages = walletManager.getNewMessages(lastNewMessagesRequestTime);
        lastNewMessagesRequestTime = System.currentTimeMillis()-1;
        for(Message message : newMessages) {
            try {
                MessageContent messageContent = new MessageContent(message.getBody());
                PublicKey authorKey = publicKeysById.get(messageContent.getAuthor());
                if (authorKey == null) {
                    log.warn("Message {} author {} not found", message.getId(), messageContent.getAuthor());
                    return;
                }
                if (messageContent.checkSign(authorKey)) {
                    log.warn("Message {} author {} signature is incorrect", message.getId(), messageContent.getAuthor());
                    return;
                }
                handleNewMessage(messageContent, message.getId());
            } catch (Exception e) {
                log.error("Can not handle message {}: {}", message.getId(), e.getMessage());
            }
        }
    }

    protected abstract void handleNewMessage(MessageContent messageContent, String messageId);
}
