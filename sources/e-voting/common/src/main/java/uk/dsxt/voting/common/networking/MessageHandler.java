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
import uk.dsxt.voting.common.messaging.Message;
import uk.dsxt.voting.common.messaging.MessageContent;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;

import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class MessageHandler {

    @FunctionalInterface
    public interface MessageReceiver {
        void handleNewMessage(MessageContent messageContent, String messageId, boolean isCommitted, String authorId);
    }

    private static final long MAX_MESSAGE_DELAY = 30 * 60 * 1000;

    private final WalletManager walletManager;

    private final CryptoHelper cryptoHelper;

    private final MessageReceiver messageReceiver;

    private final Map<String, PublicKey> publicKeysById;

    private final Set<String> handledMessageIDs = new HashSet<>();

    private long lastNewMessagesRequestTime;

    private final ScheduledExecutorService handleMessagesService = Executors.newSingleThreadScheduledExecutor();

    private final ExecutorService messagesHandler = Executors.newFixedThreadPool(10);
    
    private long totalReceivedMessageCnt = 0;

    public MessageHandler(WalletManager walletManager, CryptoHelper cryptoHelper, Map<String, PublicKey> participantKeysById, MessageReceiver messageReceiver) {
        this.walletManager = walletManager;
        this.cryptoHelper = cryptoHelper;
        this.messageReceiver = messageReceiver;
        publicKeysById = participantKeysById;

        walletManager.start();
    }

    protected MessageHandler(WalletManager walletManager, CryptoHelper cryptoHelper, Map<String, PublicKey> participantKeysById) {
        this.walletManager = walletManager;
        this.cryptoHelper = cryptoHelper;
        this.messageReceiver = null;
        publicKeysById = participantKeysById;

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
        //log.debug("checkNewMessages begins");
        long now = System.currentTimeMillis();
        List<Message> newMessages = walletManager.getNewMessages(Math.max(0, lastNewMessagesRequestTime-MAX_MESSAGE_DELAY));
        lastNewMessagesRequestTime = now;
        if (newMessages == null) {
            log.debug("checkNewMessages ends - no messages");
            return;
        }
        int skippedCnt = 0;
        for(Message message : newMessages) {
            //log.debug("checkNewMessages. id={}", message.getId());
            if (!handledMessageIDs.add(message.getId())) {
                skippedCnt++;
                //log.debug("checkNewMessages. message skipped id={}", message.getId());
            } else {
                messagesHandler.execute(() -> handleMessage(message));
            }
        }
        totalReceivedMessageCnt += newMessages.size();
        log.debug("checkNewMessages ends allCnt={}, skippedCnt={}, totalReceivedMessageCnt={}", newMessages.size(), skippedCnt, totalReceivedMessageCnt);
    }

    private void handleMessage(Message message) {
        try {
            MessageContent messageContent = new MessageContent(message.getBody());
            //log.debug("checkNewMessages. handle message id={} type={} author={}", message.getId(), messageContent.getType(), messageContent.getAuthor());
            String authorId = messageContent.getAuthor();
            PublicKey authorKey = publicKeysById.get(authorId);
            if (authorKey == null) {
                log.warn("Message {} author {} not found", message.getId(), messageContent.getAuthor());
                return;
            }
            //log.debug("checkNewMessages. message id={} has key", message.getId());
            if (!messageContent.checkSign(authorKey, cryptoHelper)) {
                log.warn("Message {} author {} signature is incorrect", message.getId(), messageContent.getAuthor());
                return;
            }
            //log.debug("checkNewMessages. message id={} signature verified", message.getId());
            handleNewMessage(messageContent, message.getId(), message.isCommitted(), authorId);
            //log.debug("checkNewMessages. message id={} handled", message.getId());
        } catch (Exception e) {
            log.error("Can not handle message {}: {}", message.getId(), e.getMessage());
        }
    }

    protected void handleNewMessage(MessageContent messageContent, String messageId, boolean isCommitted, String authorId) {
        messageReceiver.handleNewMessage(messageContent, messageId, isCommitted, authorId);
    }
}
