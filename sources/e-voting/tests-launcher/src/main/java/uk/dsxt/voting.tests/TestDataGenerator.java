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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.joda.time.Instant;
import uk.dsxt.voting.client.datamodel.ClientCredentials;
import uk.dsxt.voting.client.datamodel.ClientsOnTime;
import uk.dsxt.voting.common.domain.dataModel.*;
import uk.dsxt.voting.common.iso20022.Iso20022Serializer;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;
import uk.dsxt.voting.common.utils.crypto.KeyPair;
import uk.dsxt.voting.registriesserver.RegistriesServerMain;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Log4j2
public class TestDataGenerator {
    private final static ObjectMapper mapper = new ObjectMapper();

    //testing type
    private final static String TESTING_TYPE = "stress";

    //common settings
    private final static int PARTICIPANTS_COUNT = 100;
    private final static int NODES_COUNT = 10;
    private final static BigDecimal MAX_BLOCKED_PACKET_SIZE = new BigDecimal(30);
    private final static int DURATION_MINUTES = 60;

    //for collusion test case
    private final static int COLLUSION_PARTICIPANTS = 0;
    private final static int VICTIM_PARTICIPANTS = 0;

    //for badconnection test
    private final static int BADCONNECTION_PARTICIPANTS = 0;
    private final static int MAX_DISCONNECT_COUNT = 0;
    private final static int MAX_DISCONNECT_PERIOD = 0;

    private final static String SECURITY = "security";
    private final static String MASTER_PASSWORD = "master_password";

    private final static CryptoHelper cryptoHelper = CryptoHelper.DEFAULT_CRYPTO_HELPER;

    public static void generate() throws Exception {
        //generate voting
        long startTime = Instant.now().getMillis();
        long endTime = Instant.now().plus(DURATION_MINUTES * 60 * 1000).getMillis();
        Voting voting = generateVotingEn(startTime, endTime);
        Voting[] votings = new Voting[]{voting};
        //generating keys
        KeyPair[] keys = cryptoHelper.createCryptoKeysGenerator().generateKeys(PARTICIPANTS_COUNT);
        //generating participants
        Participant[] participants = new Participant[PARTICIPANTS_COUNT];
        for (int i = 0; i < PARTICIPANTS_COUNT; i++) {
            participants[i] = new Participant(String.valueOf(i + 1), String.format("Voter %d", i + 1), keys[i].getPublicKey());
        }
        //generate holdings
        Client[] clients = new Client[PARTICIPANTS_COUNT];
        for (int i = 0; i < PARTICIPANTS_COUNT; i++) {
            Map<String, BigDecimal> packetSizeBySecurityId = new HashMap<>();
            packetSizeBySecurityId.put(SECURITY, new BigDecimal(randomInt(15, 100)));
            clients[i] = new Client(participants[i].getId(), packetSizeBySecurityId, null);
            if ((i % (PARTICIPANTS_COUNT / NODES_COUNT)) != 0) {
                //setting nominal holder
                //TODO create tree configuration
                /*
                int nominalHolderId = i < (PARTICIPANTS_COUNT / NODES_COUNT) ? 0 : (i / (PARTICIPANTS_COUNT / NODES_COUNT)) * (PARTICIPANTS_COUNT / NODES_COUNT);
                clients[i] = new Client(clients[i].getParticipantId(), clients[i].getPacketSize(), participants[nominalHolderId].getId());
                holdings[nominalHolderId] = new Holding(holdings[nominalHolderId].getHolderId(),
                        holdings[nominalHolderId].getPacketSize().add(holdings[i].getPacketSize()), null);
                */
            }
        }
        //generating client configuration
        ClientConfiguration[] clientConfs = new ClientConfiguration[NODES_COUNT];
        for (int i = 0; i < NODES_COUNT; i++) {
            StringBuilder builderMask = new StringBuilder();
            for (int j = 0; j < (PARTICIPANTS_COUNT / NODES_COUNT); j++) {
                VoteResult vote = generateVote(i * (PARTICIPANTS_COUNT / NODES_COUNT) + j, voting, clients);
                builderMask.append(String.format("%s:%s", randomInt(1, DURATION_MINUTES - 1), vote.toString()));
                if (j != (PARTICIPANTS_COUNT / NODES_COUNT) - 1)
                    builderMask.append(";");
            }
            String mask = builderMask.toString();
            clientConfs[i] = new ClientConfiguration(participants[i * (PARTICIPANTS_COUNT / NODES_COUNT)].getId(), keys[i * (PARTICIPANTS_COUNT / NODES_COUNT)].getPrivateKey(), true, false, mask, null);
        }
        //set some of the participants as fraudsters if needed
        if (COLLUSION_PARTICIPANTS > 0 && VICTIM_PARTICIPANTS > 0) {
            List<Integer> fraudsters = new ArrayList<>();
            ThreadLocalRandom.current().ints(0, (PARTICIPANTS_COUNT / NODES_COUNT) - 1).distinct().limit(COLLUSION_PARTICIPANTS).forEach(i -> {
                makeCollusion(i, clientConfs, voting, clients);
                fraudsters.add(i);
            });
            ThreadLocalRandom.current().ints(0, (PARTICIPANTS_COUNT / NODES_COUNT) - 1).filter(i -> !fraudsters.contains(i)).distinct().limit(VICTIM_PARTICIPANTS).forEach(i -> clientConfs[i].setVictim(true));
        }
        //set disconnections
        if (BADCONNECTION_PARTICIPANTS > 0 && MAX_DISCONNECT_COUNT > 0 && MAX_DISCONNECT_PERIOD > 0) {
            ThreadLocalRandom.current().ints(0, (PARTICIPANTS_COUNT / NODES_COUNT) - 1).distinct().limit(BADCONNECTION_PARTICIPANTS).forEach(i -> makeDisconnect(i, clientConfs));
        }
        //save data to files
        saveTestData(clientConfs, votings, participants);
    }

    private static void saveTestData(ClientConfiguration[] clientConfs, Voting[] votings, Participant[] participants) throws Exception {
        final String dirPath = "/src/main/resources/json";

        String dataPath = String.format("%s/%s/%s", RegistriesServerMain.MODULE_NAME, dirPath, TESTING_TYPE);
        String votingsJson = mapper.writeValueAsString(votings);
        FileUtils.writeStringToFile(new File(String.format("%s/votings.json", dataPath)), votingsJson);
        String participantsJson = mapper.writeValueAsString(participants);
        FileUtils.writeStringToFile(new File(String.format("%s/participants.json", dataPath)), participantsJson);

        dataPath = String.format("%s/%s/%s", TestsLauncher.MODULE_NAME, dirPath, TESTING_TYPE);
        String clientConfigurationJson = mapper.writeValueAsString(clientConfs);
        FileUtils.writeStringToFile(new File(String.format("%s/clientSettings.json", dataPath)), clientConfigurationJson);
    }

    public static void main(String[] args) {
        try {
            if (args.length == 1) {
                generateCredentialsJSON();
                return;
            }
            if (args.length > 0 && args.length < 7) {
                System.out.println("<name> <totalParticipant> <holdersCount> <vmCount> <levelsCount> <minutes> <generateVotes>");
                throw new IllegalArgumentException("Invalid arguments count exception.");
            }
            int argId = 0;
            String name = args.length == 0 ? "1" : args[argId++];
            int totalParticipant = args.length == 0 ? 20 : Integer.parseInt(args[argId++]);
            int holdersCount = args.length == 0 ? 4 : Integer.parseInt(args[argId++]);
            int vmCount = args.length == 0 ? 1 : Integer.parseInt(args[argId++]);
            int levelsCount = args.length == 0 ? 3 : Integer.parseInt(args[argId++]);
            int minutes = args.length == 0 ? 3 : Integer.parseInt(args[argId++]);
            boolean generateVotes = args.length == 0 ? true : Boolean.parseBoolean(args[argId]);
            TestDataGenerator generator = new TestDataGenerator();
            generator.newGenerate(name, totalParticipant, holdersCount, vmCount, levelsCount, minutes, generateVotes);
        } catch (Exception e) {
            log.error("Test generation was failed.", e);
        }
    }

    private static void generateCredentialsJSON() throws IOException {
        List<ClientCredentials> credentials = new ArrayList<>();
        credentials.add(new ClientCredentials("user1", "1234"));
        credentials.add(new ClientCredentials("user2", "1234"));
        credentials.add(new ClientCredentials("user3", "1234"));
        credentials.add(new ClientCredentials("user4", "1234"));
        credentials.add(new ClientCredentials("user5", "1234"));
        credentials.add(new ClientCredentials("user6", "1234"));
        credentials.add(new ClientCredentials("user7", "1234"));
        credentials.add(new ClientCredentials("user8", "1234"));
        credentials.add(new ClientCredentials("user9", "1234"));
        credentials.add(new ClientCredentials("user10", "1234"));
        final String string = mapper.writeValueAsString(credentials);
        FileUtils.writeStringToFile(new File("credentials00.json"), string);
    }

    private static void makeCollusion(int i, ClientConfiguration[] clientConfs, Voting voting, Client[] clients) {
        boolean isDoubleTransaction = randomBoolean(50);
        String voteMask;
        if (isDoubleTransaction) {
            VoteResult firstVote = generateVote(i, voting, clients);
            VoteResult secondVote = generateVote(i, voting, clients);
            String firstVoteMask = String.format("%s:%s", randomInt(1, DURATION_MINUTES - 1), firstVote.toString());
            String secondVoteMask = String.format("%s:%s", randomInt(1, DURATION_MINUTES - 1), secondVote.toString());
            //send second vote
            voteMask = String.format("%s:-;%s:-", firstVoteMask, secondVoteMask);
        } else {
            VoteResult vote = generateVote(i, voting, clients);
            //send wrong participant id
            VoteResult anotherUserVote = new VoteResult(voting.getId(), clients[i == 0 ? 1 : i - 1].getParticipantId());
            anotherUserVote.getAnswersByKey().putAll(vote.getAnswersByKey());
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

    private static VoteResult generateVote(int i, Voting voting, Client[] clients) {
        VoteResult vote = new VoteResult(voting.getId(), clients[i].getParticipantId());
        BigDecimal totalSum = BigDecimal.ZERO;
        for (int j = 0; j < voting.getQuestions().length; j++) {
            String questionId = voting.getQuestions()[j].getId();
            String answerId = String.valueOf(randomInt(0, voting.getQuestions()[j].getAnswers().length - 1) + 1);
            BigDecimal voteAmount = i % (PARTICIPANTS_COUNT / NODES_COUNT) == 0 ? new BigDecimal(randomInt(0, 4)) : new BigDecimal(randomInt(0, clients[i].getPacketSizeBySecurity().get(SECURITY).subtract(totalSum).intValue()));
            totalSum = totalSum.add(voteAmount);
            vote.getAnswersByKey().put(String.valueOf(questionId), new VotedAnswer(questionId, answerId, voteAmount));
        }
        return vote;
    }

    private static VoteResult generateVote(ClientFullInfo child, Voting voting) {
        VoteResult vote = new VoteResult(voting.getId(), Integer.toString(child.getId()));
        BigDecimal totalSum = BigDecimal.ZERO;
        for (int j = 0; j < voting.getQuestions().length; j++) {
            String questionId = voting.getQuestions()[j].getId();
            String answerId = String.valueOf(randomInt(0, voting.getQuestions()[j].getAnswers().length - 1) + 1);
            int remaining = child.getPacketSizeBySecurity().get(SECURITY).subtract(totalSum).intValue();
            BigDecimal voteAmount = new BigDecimal(randomInt(Math.min(remaining / 2, remaining), remaining));
            totalSum = totalSum.add(voteAmount);
            vote.getAnswersByKey().put(String.valueOf(questionId), new VotedAnswer(questionId, answerId, voteAmount));
        }
        return vote;
    }

    private static Voting generateVotingRu(long startTime, long endTime) throws Exception {
        Question[] questions = new Question[5];
        Answer[] answers = new Answer[3];
        answers[0] = new Answer("1", "Да");
        answers[1] = new Answer("2", "Нет");
        answers[2] = new Answer("3", "Воздержался");
        questions[0] = new Question("1.1", "Выбрать в Ревизионную комиссию Общества B", answers);
        answers = new Answer[3];
        answers[0] = new Answer("1", "Да");
        answers[1] = new Answer("2", "Нет");
        answers[2] = new Answer("3", "Воздержался");
        questions[1] = new Question("1.2", "Выбрать в Ревизионную комиссию Общества A", answers);
        answers = new Answer[3];
        answers[0] = new Answer("1", "Да");
        answers[1] = new Answer("2", "Нет");
        answers[2] = new Answer("3", "Воздержался");
        questions[2] = new Question("2.1", "Избрать Совет директоров Общества. (Примечание: количественный состав Совета директоров в соответствии с Уставом – 3)", answers);
        answers = new Answer[3];
        answers[0] = new Answer("2.1.1", "Иванов");
        answers[1] = new Answer("2.1.2", "Петров");
        answers[2] = new Answer("2.1.3", "Сидоров");
        questions[3] = new Question("2.1.multi", "Избрать Совет директоров Общества. (Примечание: количественный состав Совета директоров в соответствии с Уставом – 3)", answers, true, 1);
        answers = new Answer[3];
        answers[0] = new Answer("1", "Да");
        answers[1] = new Answer("2", "Нет");
        answers[2] = new Answer("3", "Воздержался");
        questions[4] = new Question("3.1", "Утвердить  Годовой  отчет  Общества  за  2016 год, годовой бухгалтерский  баланс и счет прибылей и убытков Общества за 2016 год.", answers);
        return new Voting("1", "GMET_Ежегодное голосование", startTime, endTime, questions, SECURITY);
    }

    private static Voting generateVotingEn(long startTime, long endTime) throws Exception {
        Question[] questions = new Question[5];
        Answer[] answers = new Answer[3];
        answers[0] = new Answer("1", "For");
        answers[1] = new Answer("2", "Against");
        answers[2] = new Answer("3", "Abstain");
        questions[0] = new Question("1.1", "Elect into committee A", answers);
        answers = new Answer[3];
        answers[0] = new Answer("1", "For");
        answers[1] = new Answer("2", "Against");
        answers[2] = new Answer("3", "Abstain");
        questions[1] = new Question("1.2", "Elect into committee B", answers);
        answers = new Answer[3];
        answers[0] = new Answer("1", "For");
        answers[1] = new Answer("2", "Against");
        answers[2] = new Answer("3", "Abstain");
        questions[2] = new Question("2.1", "Elect committee directors.", answers);
        answers = new Answer[3];
        answers[0] = new Answer("2.1.1", "Ivanov");
        answers[1] = new Answer("2.1.2", "Petrov");
        answers[2] = new Answer("2.1.3", "Sidorov");
        questions[3] = new Question("2.1.multi", "Elect committee director members", answers, true, 1);
        answers = new Answer[3];
        answers[0] = new Answer("1", "For");
        answers[1] = new Answer("2", "Against");
        answers[2] = new Answer("3", "Abstain");
        questions[4] = new Question("3.1", "Approve annual document", answers);
        return new Voting("1", "GMET_Annual voting", startTime, endTime, questions, SECURITY);
    }

    private static int randomInt(int baseMinValue, int baseMaxValue) {
        Random random = new Random();
        return baseMinValue + random.nextInt(baseMaxValue - baseMinValue + 1);
    }

    private static Boolean randomBoolean(int trueProbabilityPercents) {
        int id = randomInt(0, 100);
        return id <= trueProbabilityPercents;
    }

    @Data
    @AllArgsConstructor
    class ClientFullInfo {
        Map<String, BigDecimal> packetSizeBySecurity;
        int id;
        int holderId;
        ParticipantRole role;
        String privateKey;
        String publicKey;
        String name;
        VoteResult vote;
        List<ClientFullInfo> clients;
    }

    class Recursive<FI> {
        public FI recursive;
    }

    private void newGenerate(String name, int totalParticipant, int holdersCount, int vmCount, int levelsCount, int minutes, boolean generateVotes) throws Exception {
        KeyPair[] keys = CryptoHelper.DEFAULT_CRYPTO_HELPER.createCryptoKeysGenerator().generateKeys(totalParticipant);

        ClientFullInfo[] clients = new ClientFullInfo[totalParticipant];
        Participant[] participants = new Participant[totalParticipant];
        long now = System.currentTimeMillis();
        long dayStart = now - now % (24*60*60*1000);
        Voting voting = generateVotingEn(dayStart, dayStart + minutes * 60000);

        for (int i = 0; i < totalParticipant; i++) {
            ParticipantRole role;
            if (i == 0)
                role = ParticipantRole.NRD;
            else if (i <= holdersCount)
                role = ParticipantRole.NominalHolder;
            else
                role = ParticipantRole.Owner;
            HashMap<String, BigDecimal> map = new HashMap<>();
            map.put(SECURITY, role == ParticipantRole.Owner ? new BigDecimal(randomInt(15, 100)) : BigDecimal.ZERO);
            ClientFullInfo c = new ClientFullInfo(map, i, 0, role, keys[i].getPrivateKey(), keys[i].getPublicKey(), String.format("Random name #%d", i), null, null);
            clients[i] = c;
        }
        for (int i = 0; i < totalParticipant; i++) {
            ClientFullInfo client = clients[i];
            participants[i] = new Participant(i == 0 ? "00" : Integer.toString(i), client.getName(), client.getPublicKey());
        }

        int[] counters = {1, 0, 0};
        int minNdClient = 1;

        Recursive<BiConsumer<Integer, Integer>> generateTree = new Recursive<>();
        generateTree.recursive = (id, height) -> {
            ClientFullInfo currentNode = clients[id];
            List<ClientFullInfo> children = new ArrayList<>();
            if (height + 1 != levelsCount && holdersCount > counters[0]) {
                //Generate sub nd
                int maxCount = holdersCount - counters[0];
                int undistributed = height == levelsCount ? maxCount : randomInt(1, maxCount);
                for (int i = 0; i < undistributed; i++) {
                    ClientFullInfo subND = clients[counters[0]++];
                    subND.setHolderId(id);
                    children.add(subND);
                }
            }
            //Generate owners
            int maxCount = (totalParticipant - holdersCount - counters[1]) - (holdersCount - counters[2]) * minNdClient;
            int undistributed = randomInt(minNdClient, maxCount);
            for (int i = 0; i < undistributed; i++) {
                ClientFullInfo child = clients[holdersCount + counters[1]++];
                child.setVote(generateVote(child, voting));
                children.add(child);
            }
            counters[2]++;
            currentNode.setClients(children);
            //Generate next level
            for (ClientFullInfo child : children) {
                if (child.getRole() != ParticipantRole.Owner)
                    generateTree.recursive.accept(child.getId(), height + 1);
            }
            currentNode.setPacketSizeBySecurity(children.stream().map(ClientFullInfo::getPacketSizeBySecurity).reduce(new HashMap<>(), (map1, map2) -> {
                for (String key : map2.keySet()) {
                    BigDecimal old = map1.get(key);
                    map1.put(key, (old == null ? BigDecimal.ZERO : old).add(map2.get(key)));
                }
                return map1;
            }));
        };

        generateTree.recursive.accept(0, 0);

        final String dirPath = "/src/main/resources/scenarios";
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s/participants.json", BaseTestsLauncher.MODULE_NAME, dirPath, name)), mapper.writeValueAsString(participants));
        Iso20022Serializer serializer = new Iso20022Serializer();
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s/voting.xml", BaseTestsLauncher.MODULE_NAME, dirPath, name)), serializer.serialize(voting));
        StringBuilder vmConfig = new StringBuilder();
        int countByVM = (holdersCount + vmCount - 1) / vmCount;
        int totalCount = 0;
        for (int i = 0; i < vmCount; i++) {
            int count = Math.min(holdersCount - totalCount, countByVM);
            vmConfig.append(String.format("%s=%s%n", i, count));
            totalCount += count;
        }
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s/vm.txt", BaseTestsLauncher.MODULE_NAME, dirPath, name)), vmConfig.toString());
        StringBuilder nodesConfig = new StringBuilder();
        for (int i = 0; i < holdersCount; i++) {
            ClientFullInfo client = clients[i];
            List<ClientCredentials> credentials = client.getClients().stream().
                map(child -> new ClientCredentials(Integer.toString(child.getId()), Integer.toString(child.getId()))).
                collect(Collectors.toList());
            FileUtils.writeStringToFile(new File(String.format("%s/%s/%s/%s/credentials.json", BaseTestsLauncher.MODULE_NAME, dirPath, name, client.getId())), mapper.writeValueAsString(credentials));
            List<Client> clientsJson = client.getClients().stream().
                map(child -> new Client(Integer.toString(child.getId()), child.getPacketSizeBySecurity(), child.getRole())).
                collect(Collectors.toList());
            FileUtils.writeStringToFile(new File(String.format("%s/%s/%s/%s/clients.json", BaseTestsLauncher.MODULE_NAME, dirPath, name, client.getId())),
                mapper.writeValueAsString(new ClientsOnTime[]{new ClientsOnTime(-20000, clientsJson.toArray(new Client[clientsJson.size()]))}));
            String messages = client.getClients().stream().
                filter(child -> child.getVote() != null).
                map(child -> String.format("%s:%s", randomInt(30, minutes * 60), child.getVote().toString())).
                reduce("", (s1, s2) -> s1 + "\n" + s2);
            FileUtils.writeStringToFile(new File(String.format("%s/%s/%s/%s/messages.txt", BaseTestsLauncher.MODULE_NAME, dirPath, name, client.getId())), generateVotes ? messages : "");

            nodesConfig.append(i);
            nodesConfig.append("=");
            nodesConfig.append(mapper.writeValueAsString(new NodeInfo(client.getId() == 0 ? MASTER_PASSWORD : "", client.getId(), client.getHolderId(), client.getPrivateKey(), null)));
            nodesConfig.append("\n");
        }
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s/voting.txt", BaseTestsLauncher.MODULE_NAME, dirPath, name)), nodesConfig.toString());
    }
}
