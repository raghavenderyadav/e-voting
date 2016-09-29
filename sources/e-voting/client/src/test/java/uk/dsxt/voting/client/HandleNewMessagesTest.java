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

import lombok.extern.log4j.Log4j2;
import org.junit.Ignore;
import org.junit.Test;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.dataModel.Question;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.domain.nodes.AssetsHolder;
import uk.dsxt.voting.common.domain.nodes.MasterNode;
import uk.dsxt.voting.common.domain.nodes.VotingOrganizer;
import uk.dsxt.voting.common.iso20022.Iso20022Serializer;
import uk.dsxt.voting.common.messaging.MessageContent;
import uk.dsxt.voting.common.messaging.MessagesSerializer;
import uk.dsxt.voting.common.networking.MockWalletManager;
import uk.dsxt.voting.common.networking.WalletManager;
import uk.dsxt.voting.common.networking.WalletMessageConnector;
import uk.dsxt.voting.common.utils.MessageBuilder;
import uk.dsxt.voting.common.utils.crypto.CryptoHelperImpl;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Ignore
public class HandleNewMessagesTest {
    
    @Test
    public void testVotingOrganizer() throws Exception {
        CryptoHelperImpl cryptoHelper = CryptoHelperImpl.DEFAULT_CRYPTO_HELPER;
        WalletManager walletManager = new MockWalletManager();
        MessagesSerializer messagesSerializer = new Iso20022Serializer();
        Map<String, PublicKey> participantKeysById = new HashMap();
        PrivateKey privateKey = cryptoHelper.loadPrivateKey("MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCOcmezdk84J9QUIPl6URnlJrnCybBTsluqVcWxddVARLLE1k2dk5AA4i7gJ1xHapKAQL7F3XjcTZplitWfP2jxiTMMBcYqMG31dy5f90v+B+4juwQvpJoISYhXaBUtZxcos016ZcY8ylXTpqXTt5MCSXoSMvfSGdBE74acodG6sLXwnukNqOBSgU/SJkVWPxtjb14cEF2CXRQklNKs83B03ELDU7+08teFh89Mon9qHiUR8WqCAPMaTm0qWIbLvxGr99LZRLgxCTe8Hgj2Md2oT85voQdueldll/EKP6VpIBgzOqjBoMc/3cUjd8G5fmdQZEPK2teWE4gO9G59cTEFAgMBAAECggEAOuTiKzjHGBiffpMDkqblZfDU7MwmsvQTIiHEUtK9EI1WvDs+a+AOsc7SQqsDZCOT3qLmPTiMN8l+BG2aVPUKlpJ7IIVioR7U16Am9FZyfN0agHtaB7iuVq7QSBMoblUpJhK7/dcGVyvwwEkuVpKXnWJzrgKUo7E4gsflh+z/oYe/xHkeIAZG1C9r19ntR7s8sE/lACoD3TxZDmGbOXuoBQLTz0TYXWsNMguu7DKflp41sD8SybOfe9z8Tfq3rTqpcBZivHbtGzgR/dJxiInqtDwmAPU1xLr6Cv9KK53/UHAs557TbrBzVAAqUxSjpReA08OxYhRneHIJY8MGjU9WIQKBgQDPE/vRst6D6EK0z1nvESPuJqZQkMg5RSYu9nIMRKYV9kZl9GJ5kTaHtM27URd5ctAXHiqJYEQEIfBLwxAXksyC1jLWd3X34Zl+omzNy1+HjbwInMhifjW7nfwJXJVfKhHZFORLkCBLL5xt1erazbEeYiTfiQ9Z0g3XvveG3D1YfwKBgQCwGYx3owGfgnKbPpsCIVm3v2zd7Rh8c6HcibeomTcK0tVrkKfi4Kk9ed+iOytf+Up2HtBzJqp64R8Xs2eFccOOzx6tJN82pzcP48J4XIXQyuxpHEheQABLACHwy+WUVqR0jsUQf/7934v1Fv07B1woGMIaDjFJffFDkbQYrhJUewKBgQCNR7wSCPBJnKgORj28nrwd2l60LuN8N1Jizh9ngVqzNzA2lTKucEV89v06JIxYft28OAebbINbMnCIsBAFlVFUnqFWs3BX66JWxKhpC60kha3ZTmZk1GkClToEhRcgM0q0Cc3sQ+vUgCpAwacXGykRarJvlEpV5LsvDApDB3YPLwKBgGVHjF4SRhCzOa7Hpubmv27KjZZlkjuhVWo9Wn+A/wMeltgybhwyEaPlwBTR6vRbr9OXjVNs3YemifdbmyJId6xeusnh9u675RMibupCbEPVMXqSZZyvOnvoK50N55AU9KiEpBoFQ2ZHd3sSKboVVY9KDfhmSTp3UJcH6Yh4NNqZAoGBALRYToCYl7NJR2iGExW1nLlTWT2+LzqMb/wI32MOfKQefgiW2tYDk1GyULhCIkbEGMLsVqvziF8ntH3OwydLX30OrZwnOgkbKV+FiIBQ1qL7H8cMjBwC4b6C1JhfBp8OpNd3zu1zFAZrvEQRleKTASmPLJhLbEqydYSV4XJsgDl7");
        String publicKey ="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjnJns3ZPOCfUFCD5elEZ5Sa5wsmwU7JbqlXFsXXVQESyxNZNnZOQAOIu4CdcR2qSgEC+xd143E2aZYrVnz9o8YkzDAXGKjBt9XcuX/dL/gfuI7sEL6SaCEmIV2gVLWcXKLNNemXGPMpV06al07eTAkl6EjL30hnQRO+GnKHRurC18J7pDajgUoFP0iZFVj8bY29eHBBdgl0UJJTSrPNwdNxCw1O/tPLXhYfPTKJ/ah4lEfFqggDzGk5tKliGy78Rq/fS2US4MQk3vB4I9jHdqE/Ob6EHbnpXZZfxCj+laSAYMzqowaDHP93FI3fBuX5nUGRDytrXlhOIDvRufXExBQIDAQAB";
        participantKeysById.put("00", cryptoHelper.loadPublicKey(publicKey));
        WalletMessageConnector walletMessageConnector = new WalletMessageConnector(walletManager, messagesSerializer, cryptoHelper,
            participantKeysById, privateKey, "00", MasterNode.MASTER_HOLDER_ID, 10000000, 10);
        VotingOrganizer votingOrganizer = new VotingOrganizer(messagesSerializer, cryptoHelper, participantKeysById, privateKey, 10000000);
        walletMessageConnector.addClient(votingOrganizer);
        Voting voting = new Voting("v0", "v0", "GMET", System.currentTimeMillis(), System.currentTimeMillis() + 10000000, new Question[0], "sec");
        votingOrganizer.addVoting(voting);
        VoteResult result = new VoteResult("vo", "00", BigDecimal.TEN);
        String serializedVote = messagesSerializer.serialize(result, voting);
        String voteMessage = MessageBuilder.buildMessage(serializedVote, AssetsHolder.EMPTY_SIGNATURE);
        String encryptedVoteMessage = cryptoHelper.encrypt(voteMessage, cryptoHelper.loadPublicKey(publicKey));
        Map<String, String> content = new HashMap();
        content.put("BODY", encryptedVoteMessage);
        MessageContent messageContent = new MessageContent(MessageContent.buildOutputMessage("VOTE", "00", privateKey, cryptoHelper, content));
        for(int i = 0; i < 100000; i++) {
            walletMessageConnector.handleNewMessage(messageContent, Integer.toString(i), true, "00");
        }
        log.info("all messages sent");
        Thread.sleep(10000000);
    }
}
