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

package uk.dsxt.voting.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.joda.time.Instant;
import uk.dsxt.voting.common.datamodel.*;
import uk.dsxt.voting.common.utils.CryptoKeysGenerator;
import uk.dsxt.voting.registriesserver.RegistriesServerMain;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class TestDataGenerator {
    private final static ObjectMapper mapper = new ObjectMapper();

    //testing type
    private final static String TESTING_TYPE = "stress";

    //common settings
    private final static int COUNT = 100;
    private final static BigDecimal MAX_BLOCKED_PACKET_SIZE = new BigDecimal(30);
    private final static int DURATION_MINUTES = 60;

    //for collusion test case
    private final static int COLLUSION_PARTICIPANTS = 0;

    public static void generate() throws Exception {
        //generate voting
        long startTime = Instant.now().getMillis();
        long endTime = Instant.now().plus(DURATION_MINUTES * 60 * 1000).getMillis();
        Voting voting = generateVoting(startTime, endTime);
        Voting[] votings = new Voting[]{voting};
        //generating keys
        KeyPair[] keys = CryptoKeysGenerator.generateKeys(COUNT);
        //generating participants
        Participant[] participants = new Participant[COUNT];
        for (int i = 0; i < COUNT; i++) {
            participants[i] = new Participant(String.valueOf(i + 1), String.format("Voter %d", i + 1), keys[i].getPublicKey());
        }
        //generate holdings
        Holding[] holdings = new Holding[COUNT];
        for (int i = 0; i < COUNT; i++) {
            holdings[i] = new Holding(participants[i].getId(), new BigDecimal(randomInt(1, 100)), null);
            if (i % 10 == 0 && i != 0) {
                //setting nominal holder
                int nominalHolderId = i - 1;
                holdings[i] = new Holding(holdings[i].getHolderId(), holdings[i].getPacketSize(), participants[nominalHolderId].getId());
                holdings[nominalHolderId] = new Holding(holdings[nominalHolderId].getHolderId(),
                        holdings[nominalHolderId].getPacketSize().add(holdings[i].getPacketSize()), null);
            }
        }
        //generating blacklist
        List<BlockedPacket> blackListEntries = new ArrayList<>();
        for (int i = 0; i < holdings.length; i++) {
            if ((i + 1) % 5 == 0) {
                Holding holding = holdings[i];
                blackListEntries.add(new BlockedPacket(holding.getHolderId(), holding.getPacketSize().compareTo(MAX_BLOCKED_PACKET_SIZE) < 0 ? holding.getPacketSize() : MAX_BLOCKED_PACKET_SIZE));
            }
        }
        BlockedPacket[] blackList = blackListEntries.toArray(new BlockedPacket[blackListEntries.size()]);
        //generating client configuration
        ClientConfiguration[] clientConfs = new ClientConfiguration[COUNT];
        for (int i = 0; i < COUNT; i++) {
            VoteResult vote = generateVote(i, voting, holdings, participants);
            clientConfs[i] = new ClientConfiguration(participants[i].getId(), keys[i].getPrivateKey(), true, String.format("%s:%s", randomInt(1, DURATION_MINUTES - 1), vote.toString()), null, null);
        }
        //set some of the participants as fraudsters if needed
        if (COLLUSION_PARTICIPANTS > 0) {
            ThreadLocalRandom.current().ints(0, COUNT).distinct().limit(COLLUSION_PARTICIPANTS).forEach(i -> makeCollusion(i, clientConfs, voting, holdings, participants));
        }
        //save data to files
        saveTestData(clientConfs, votings, holdings, participants, blackList);
    }

    private static void saveTestData(ClientConfiguration[] clientConfs, Voting[] votings, Holding[] holdings, Participant[] participants, BlockedPacket[] blackList) throws Exception{
        final String dirPath = "/src/main/resources/json";

        String dataPath = String.format("%s/%s/%s", RegistriesServerMain.MODULE_NAME, dirPath, TESTING_TYPE);
        String votingsJson = mapper.writeValueAsString(votings);
        FileUtils.writeStringToFile(new File(String.format("%s/votings.json", dataPath)), votingsJson);
        String participantsJson = mapper.writeValueAsString(participants);
        FileUtils.writeStringToFile(new File(String.format("%s/participants.json", dataPath)), participantsJson);
        String holdingsJson = mapper.writeValueAsString(holdings);
        FileUtils.writeStringToFile(new File(String.format("%s/holdings.json", dataPath)), holdingsJson);
        String blackListJson = mapper.writeValueAsString(blackList);
        FileUtils.writeStringToFile(new File(String.format("%s/blacklist.json", dataPath)), blackListJson);

        dataPath = String.format("%s/%s/%s", TestsLauncher.MODULE_NAME, dirPath, TESTING_TYPE);
        String clientConfigurationJson = mapper.writeValueAsString(clientConfs);
        FileUtils.writeStringToFile(new File(String.format("%s/clientSettings.json", dataPath)), clientConfigurationJson);
    }

    private static void makeCollusion(int i, ClientConfiguration[] clientConfs, Voting voting, Holding[] holdings, Participant[] participants) {
        VoteResult vote = generateVote(i, voting, holdings, participants);

        boolean isDoubleTransaction = randomBoolean(50);
        String voteMask = String.format("%s:%s", randomInt(1, DURATION_MINUTES - 1), vote.toString());
        if (isDoubleTransaction) {
            //send second vote
            voteMask = String.format("%s:-%s%s:-", clientConfs[i].getVote(), System.lineSeparator(), voteMask);
        } else {
            //send wrong participant id
            VoteResult anotherUserVote = new VoteResult(voting.getId(), participants[i == 0 ? 1 : i - 1].getId());
            anotherUserVote.getAnswersByQuestionId().putAll(vote.getAnswersByQuestionId());
            voteMask = String.format("%s:%s:-", randomInt(1, DURATION_MINUTES - 1), anotherUserVote.toString());
        }

        clientConfs[i] = new ClientConfiguration(clientConfs[i].getHolderId(), clientConfs[i].getPrivateKey(), false, voteMask, null, null);
    }

    private static VoteResult generateVote(int i, Voting voting, Holding[] holdings, Participant[] participants) {
        VoteResult vote = new VoteResult(voting.getId(), participants[i].getId());
        BigDecimal totalSum = BigDecimal.ZERO;
        for (int j = 0; j < voting.getQuestions().length; j++) {
            int questionId = voting.getQuestions()[j].getId();
            int answerId = randomInt(0, voting.getQuestions()[j].getAnswers().length - 1) + 1;
            BigDecimal voteAmount = new BigDecimal(randomInt(0, holdings[i].getPacketSize().subtract(totalSum).intValue()));
            totalSum = totalSum.add(voteAmount);
            vote.getAnswersByQuestionId().put(String.valueOf(questionId), new VotedAnswer(questionId, answerId, voteAmount));
        }
        return vote;
    }

    private static Voting generateVoting(long startTime, long endTime) throws Exception {
        Question[] questions = new Question[3];
        Answer[] answers = new Answer[3];
        answers[0] = new Answer(1, "Yes");
        answers[1] = new Answer(2, "No");
        answers[2] = new Answer(3, "Abstained");
        questions[0] = new Question(1, "Do you approve the results of work?", answers);
        questions[1] = new Question(2, "Will you elect a new chairman?", answers);

        answers = new Answer[3];
        answers[0] = new Answer(1, "Ivanov");
        answers[1] = new Answer(2, "Petrov");
        answers[2] = new Answer(3, "Sidorov");
        questions[2] = new Question(3, "New chairman", answers);
        return new Voting("1", "The annual voting of shareholders of OJSC 'Blockchain Company'",
                startTime, endTime, questions);
    }

    private static int randomInt(int baseMinValue, int baseMaxValue) {
        Random random = new Random();
        return baseMinValue + random.nextInt(baseMaxValue - baseMinValue + 1);
    }

    private static Boolean randomBoolean(int trueProbabilityPercents) {
        int id = randomInt(0, 100);
        return id <= trueProbabilityPercents;
    }
}
