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

package uk.dsxt.voting.masterclient;

import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.common.datamodel.Participant;
import uk.dsxt.voting.common.networking.MessageContent;
import uk.dsxt.voting.common.networking.MessageHandler;
import uk.dsxt.voting.common.networking.WalletManager;

import java.math.BigDecimal;
import java.util.*;

@Log4j2
public class MoneyDistributer extends MessageHandler {

    private final BigDecimal moneyToNode;

    private final Set<String> sentIds = new HashSet<>();

    public MoneyDistributer(WalletManager walletManager, Participant[] participants, BigDecimal moneyToNode) {
        super(walletManager, participants);
        this.moneyToNode = moneyToNode;
    }

    @Override
    protected void handleNewMessage(MessageContent messageContent, String messageId) {
        if (MessageContent.TYPE_INITIAL_MONEY_REQUEST.equals(messageContent.getType())) {
            log.info("Message {} contains initial money request", messageId);
            if (sentIds.contains(messageContent.getAuthor())) {
                log.warn("Message {} author {} already has money", messageId, messageContent.getAuthor());
                return;
            }
            String wallet = messageContent.getField(MessageContent.FIELD_WALLET);
            walletManager.sendMoneyToAddressBalance(moneyToNode, wallet);
            log.info("{} money sent to {}", moneyToNode, wallet);
            sentIds.add(messageContent.getAuthor());
        }
    }
}
