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
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TestDataGenerator {

    public static void main(String[] args) throws Exception {
        TestDataGenerator.generate();
    }

    private final static ObjectMapper mapper = new ObjectMapper();

    //testing type
    private final static String TESTING_TYPE = "stress";

    //common settings
    private final static int PARTICIPANTS_COUNT = 100;
    private final static int NODES_COUNT = 10;
    private final static int EMPTY_NODES_COUNT = 0;
    private final static BigDecimal MAX_BLOCKED_PACKET_SIZE = new BigDecimal(30);
    private final static int DURATION_MINUTES = 60;

    //for collusion test case
    private final static int COLLUSION_PARTICIPANTS = 0;
    private final static int VICTIM_PARTICIPANTS = 0;

    //for badconnection test
    private final static int BADCONNECTION_PARTICIPANTS = 0;
    private final static int MAX_DISCONNECT_COUNT = 0;
    private final static int MAX_DISCONNECT_PERIOD = 0;

    public static void generate() throws Exception {
        //generate voting
        long startTime = Instant.now().getMillis();
        long endTime = Instant.now().plus(DURATION_MINUTES * 60 * 1000).getMillis();
        Voting voting = generateVoting(startTime, endTime);
        Voting[] votings = new Voting[]{voting};
        //generating keys
        KeyPair[] keys = CryptoKeysGenerator.generateKeys(PARTICIPANTS_COUNT);
        //generating participants
        Participant[] participants = new Participant[PARTICIPANTS_COUNT];
        for (int i = 0; i < PARTICIPANTS_COUNT; i++) {
            participants[i] = new Participant(String.valueOf(i + 1), String.format("Voter %d", i + 1), keys[i].getPublicKey());
        }
        //generate holdings
        Holding[] holdings = new Holding[PARTICIPANTS_COUNT];
        for (int i = 0; i < PARTICIPANTS_COUNT; i++) {
            holdings[i] = new Holding(participants[i].getId(), new BigDecimal(randomInt(15, 100)), null);
            if ((i % (PARTICIPANTS_COUNT / NODES_COUNT)) != 0) {
                //setting nominal holder
                int nominalHolderId = i < (PARTICIPANTS_COUNT / NODES_COUNT) ? 0 : (i / (PARTICIPANTS_COUNT / NODES_COUNT)) * (PARTICIPANTS_COUNT / NODES_COUNT);
                holdings[i] = new Holding(holdings[i].getHolderId(), holdings[i].getPacketSize(), participants[nominalHolderId].getId());
                holdings[nominalHolderId] = new Holding(holdings[nominalHolderId].getHolderId(),
                        holdings[nominalHolderId].getPacketSize().add(holdings[i].getPacketSize()), null);
            }
        }
        //generating blacklist
        List<BlockedPacket> blackListEntries = new ArrayList<>();
        Map<String, BigDecimal> blockedPacketByHolderId = new HashMap<>();
        for (int i = 0; i < holdings.length; i++) {
            if ((i + 1) % (PARTICIPANTS_COUNT / NODES_COUNT) == 0) {
                Holding holding = holdings[i];
                BigDecimal blockedPacket = holding.getPacketSize().compareTo(MAX_BLOCKED_PACKET_SIZE) < 0 ? holding.getPacketSize() : MAX_BLOCKED_PACKET_SIZE;
                blackListEntries.add(new BlockedPacket(holding.getHolderId(), blockedPacket));
                blockedPacketByHolderId.put(holding.getHolderId(), blockedPacket);
            }
        }
        BlockedPacket[] blackList = blackListEntries.toArray(new BlockedPacket[blackListEntries.size()]);
        //generating client configuration
        ClientConfiguration[] clientConfs = new ClientConfiguration[NODES_COUNT + EMPTY_NODES_COUNT];
        for (int i = 0; i < NODES_COUNT; i++) {
            StringBuilder builderMask = new StringBuilder();
            for (int j = 0; j < (PARTICIPANTS_COUNT / NODES_COUNT); j++) {
                VoteResult vote = generateVote(i * (PARTICIPANTS_COUNT / NODES_COUNT) + j, voting, holdings, participants, blockedPacketByHolderId);
                builderMask.append(String.format("%s:%s", randomInt(1, DURATION_MINUTES - 1), vote.toString()));
                if (j != (PARTICIPANTS_COUNT / NODES_COUNT) - 1)
                    builderMask.append(";");
            }
            String mask = builderMask.toString();
            clientConfs[i] = new ClientConfiguration(participants[i * (PARTICIPANTS_COUNT / NODES_COUNT)].getId(), keys[i * (PARTICIPANTS_COUNT / NODES_COUNT)].getPrivateKey(), true, false, mask, null);
        }
        for (int i = NODES_COUNT; i < NODES_COUNT + EMPTY_NODES_COUNT; i++) {
            clientConfs[i] = new ClientConfiguration(participants[i].getId(), keys[i].getPrivateKey(), true, false, null, null);
        }

        //set some of the participants as fraudsters if needed
        if (COLLUSION_PARTICIPANTS > 0 && VICTIM_PARTICIPANTS > 0) {
            List<Integer> fraudsters = new ArrayList<>();
            ThreadLocalRandom.current().ints(0, (PARTICIPANTS_COUNT / NODES_COUNT) - 1).distinct().limit(COLLUSION_PARTICIPANTS).forEach(i -> {
                makeCollusion(i, clientConfs, voting, holdings, participants, blockedPacketByHolderId);
                fraudsters.add(i);
            });
            ThreadLocalRandom.current().ints(0, (PARTICIPANTS_COUNT / NODES_COUNT) - 1).filter(i -> !fraudsters.contains(i)).distinct().limit(VICTIM_PARTICIPANTS).forEach(i -> clientConfs[i].setVictim(true));
        }
        //set disconnections
        if (BADCONNECTION_PARTICIPANTS > 0 && MAX_DISCONNECT_COUNT > 0 && MAX_DISCONNECT_PERIOD > 0) {
            ThreadLocalRandom.current().ints(0, (PARTICIPANTS_COUNT / NODES_COUNT) - 1).distinct().limit(BADCONNECTION_PARTICIPANTS).forEach(i -> makeDisconnect(i, clientConfs));
        }
        //save data to files
        saveTestData(clientConfs, votings, holdings, participants, blackList);
    }

    private static void saveTestData(ClientConfiguration[] clientConfs, Voting[] votings, Holding[] holdings, Participant[] participants, BlockedPacket[] blackList) throws Exception {
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

    private static void makeCollusion(int i, ClientConfiguration[] clientConfs, Voting voting, Holding[] holdings, Participant[] participants, Map<String, BigDecimal> blockedPacketByHolderId) {
        boolean isDoubleTransaction = randomBoolean(50);
        String voteMask = null;
        if (isDoubleTransaction) {
            VoteResult firstVote = generateVote(i, voting, holdings, participants, blockedPacketByHolderId);
            VoteResult secondVote = generateVote(i, voting, holdings, participants, blockedPacketByHolderId);
            String firstVoteMask = String.format("%s:%s", randomInt(1, DURATION_MINUTES - 1), firstVote.toString());
            String secondVoteMask = String.format("%s:%s", randomInt(1, DURATION_MINUTES - 1), secondVote.toString());
            //send second vote
            voteMask = String.format("%s:-;%s:-", firstVoteMask, secondVoteMask);
        } else {
            VoteResult vote = generateVote(i, voting, holdings, participants, blockedPacketByHolderId);
            //send wrong participant id
            VoteResult anotherUserVote = new VoteResult(voting.getId(), participants[i == 0 ? 1 : i - 1].getId());
            anotherUserVote.getAnswersByQuestionId().putAll(vote.getAnswersByQuestionId());
            voteMask = String.format("%s:%s:-", randomInt(1, DURATION_MINUTES - 1), anotherUserVote.toString());
        }
        clientConfs[i] = new ClientConfiguration(clientConfs[i].getHolderId(), clientConfs[i].getPrivateKey(), false, false, voteMask, null);
    }

    private static void makeDisconnect(int i, ClientConfiguration[] clientConfs) {
        StringBuilder maskBuilder = new StringBuilder();
        int disconnectCount = randomInt(1, MAX_DISCONNECT_COUNT);
        int startDisconnectTime = randomInt(1, DURATION_MINUTES / MAX_DISCONNECT_COUNT);
        for (int j = 0; j < disconnectCount; j++) {
            int period = randomInt(1, MAX_DISCONNECT_PERIOD);
            int endDisconnectTime = Math.min(startDisconnectTime + period, DURATION_MINUTES - 1);
            maskBuilder.append(String.format("%s-%s", startDisconnectTime, endDisconnectTime));
            if (endDisconnectTime == DURATION_MINUTES - 1)
                break;
            startDisconnectTime = endDisconnectTime + 1;
            if (j < disconnectCount - 1)
                maskBuilder.append(";");
        }
        clientConfs[i].setDisconnectMask(maskBuilder.toString());
    }

    private static VoteResult generateVote(int i, Voting voting, Holding[] holdings, Participant[] participants, Map<String, BigDecimal> blockedPacketByHolderId) {
        VoteResult vote = new VoteResult(voting.getId(), participants[i].getId());
        BigDecimal totalSum = blockedPacketByHolderId.containsKey(holdings[i].getHolderId()) ? blockedPacketByHolderId.get(holdings[i].getHolderId()) : BigDecimal.ZERO;
        for (int j = 0; j < voting.getQuestions().length; j++) {
            int questionId = voting.getQuestions()[j].getId();
            int answerId = randomInt(0, voting.getQuestions()[j].getAnswers().length - 1) + 1;
            BigDecimal voteAmount = i % (PARTICIPANTS_COUNT / NODES_COUNT) == 0 ? new BigDecimal(randomInt(0, 4)) : new BigDecimal(randomInt(0, holdings[i].getPacketSize().subtract(totalSum).intValue()));
            totalSum = totalSum.add(voteAmount);
            vote.getAnswersByQuestionId().put(String.valueOf(questionId), new VotedAnswer(questionId, answerId, voteAmount));
        }
        return vote;
    }

    private static Voting generateVoting(long startTime, long endTime) throws Exception {
        Question[] questions = new Question[3];
        Answer[] answers = new Answer[3];
        answers[0] = new Answer(1, "Temnov");
        answers[1] = new Answer(2, "Svetlov");
        answers[2] = new Answer(3, "Vertev");
        questions[0] = new Question(1, "New member", answers);

        answers = new Answer[3];
        answers[0] = new Answer(1, "Tsrev");
        answers[1] = new Answer(2, "Bronev");
        answers[2] = new Answer(3, "Steklov");
        questions[1] = new Question(2, "New vice-president", answers);
        answers = new Answer[3];
        answers[0] = new Answer(1, "Ivanov");
        answers[1] = new Answer(2, "Petrov");
        answers[2] = new Answer(3, "Sidorov");
        questions[2] = new Question(3, "New chairman", answers);
        return new Voting("1", "The annual voting of shareholders of OJSC 'Blockchain Company'", startTime, endTime, questions);

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
