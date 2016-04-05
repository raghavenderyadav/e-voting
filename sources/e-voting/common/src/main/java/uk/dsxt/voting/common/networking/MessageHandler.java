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
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.messaging.Message;
import uk.dsxt.voting.common.messaging.MessageContent;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class MessageHandler {

    @FunctionalInterface
    public interface MessageReceiver {
        void handleNewMessage(MessageContent messageContent, String messageId);
    }

    private static final long MAX_MESSAGE_DELAY = 10 * 60 * 1000;

    protected final WalletManager walletManager;

    private final CryptoHelper cryptoHelper;

    private final MessageReceiver messageReceiver;

    private final Map<String, PublicKey> publicKeysById = new HashMap<>();

    private final Set<String> handledMessageIDs = new HashSet<>();

    private long lastNewMessagesRequestTime;

    private final ScheduledExecutorService handleMessagesService = Executors.newSingleThreadScheduledExecutor();

    public MessageHandler(WalletManager walletManager, CryptoHelper cryptoHelper, Participant[] participants, MessageReceiver messageReceiver) {
        this.walletManager = walletManager;
        this.cryptoHelper = cryptoHelper;
        this.messageReceiver = messageReceiver;
        for(Participant participant : participants) {
            if (participant.getPublicKey() != null) {
                try {
                    PublicKey key = cryptoHelper.loadPublicKey(participant.getPublicKey());
                    publicKeysById.put(participant.getId(), key);
                } catch (GeneralSecurityException e) {
                    log.error("Can not extract public key for participant {}({})", participant.getName(), participant.getId());
                }
            }
        }

        walletManager.start();
    }

    protected MessageHandler(WalletManager walletManager, CryptoHelper cryptoHelper, Participant[] participants) {
        this.walletManager = walletManager;
        this.cryptoHelper = cryptoHelper;
        this.messageReceiver = null;
        for(Participant participant : participants) {
            if (participant.getPublicKey() != null) {
                try {
                    PublicKey key = cryptoHelper.loadPublicKey(participant.getPublicKey());
                    publicKeysById.put(participant.getId(), key);
                } catch (GeneralSecurityException e) {
                    log.error("Can not extract public key for participant {}({})", participant.getName(), participant.getId());
                }
            }
        }

        walletManager.start();
    }

    public void run(long newMessagesRequestInterval) {
        checkNewMessages();
        handleMessagesService.scheduleWithFixedDelay(() -> {
            try {
                checkNewMessages();
            } catch (Exception e) {
                log.error("handle messages failed", e);
            }
        }, 0, newMessagesRequestInterval, TimeUnit.MILLISECONDS);
        log.info("MessageHandler runs");
    }

    public void stop() {
        handleMessagesService.shutdownNow();
        log.info("MessageHandler stopped");
    }

    private void checkNewMessages() {
        long now = System.currentTimeMillis();
        List<Message> newMessages = walletManager.getNewMessages(Math.max(0, lastNewMessagesRequestTime-MAX_MESSAGE_DELAY));
        lastNewMessagesRequestTime = now;
        if (newMessages == null)
            return;
        for(Message message : newMessages) {
            if (!handledMessageIDs.add(message.getId())) {
                continue;
            }
            try {
                MessageContent messageContent = new MessageContent(message.getBody());
                PublicKey authorKey = publicKeysById.get(messageContent.getAuthor());
                if (authorKey == null) {
                    log.warn("Message {} author {} not found", message.getId(), messageContent.getAuthor());
                    continue;
                }
                if (!messageContent.checkSign(authorKey, cryptoHelper)) {
                    log.warn("Message {} author {} signature is incorrect", message.getId(), messageContent.getAuthor());
                    continue;
                }
                handleNewMessage(messageContent, message.getId());
            } catch (Exception e) {
                log.error("Can not handle message {}: {}", message.getId(), e.getMessage());
            }
        }
    }

    protected void handleNewMessage(MessageContent messageContent, String messageId) {
        messageReceiver.handleNewMessage(messageContent, messageId);
    }
}
