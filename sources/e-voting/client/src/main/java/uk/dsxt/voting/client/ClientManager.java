package uk.dsxt.voting.client;

import lombok.Value;
import uk.dsxt.voting.client.datamodel.QuestionWeb;
import uk.dsxt.voting.client.datamodel.VotingInfoWeb;
import uk.dsxt.voting.client.datamodel.VotingWeb;
import uk.dsxt.voting.common.domain.dataModel.Client;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.nodes.AssetsHolder;
import uk.dsxt.voting.common.iso20022.jaxb.MeetingInstruction;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Value
public class ClientManager {
    ConcurrentMap<String, Participant> participantsById = new ConcurrentHashMap<>();
    ConcurrentMap<String,Client> clientsById = new ConcurrentHashMap<>();
    ConcurrentMap<String, VotingWeb> votingsById = new ConcurrentHashMap<>();

    MeetingInstruction participantsXml;


    AssetsHolder assetsHolder;

    public ClientManager(AssetsHolder assetsHolder, MeetingInstruction participantsXml) {
        this.assetsHolder = assetsHolder;
        this.participantsXml = participantsXml;
    }

    public Participant[] getParticipants() {
        return participantsById.values().toArray(new Participant[0]);
    }

    public Client[] getClients() {
        return clientsById.values().toArray(new Client[0]);
    }

    public VotingWeb[] getVotings() {
        return votingsById.values().toArray(new VotingWeb[0]);
    }

    public VotingInfoWeb getVoting(String votingId) {
        return new VotingInfoWeb(votingsById.get(votingId) == null ? null : votingsById.get(votingId).getQuestions(), new BigDecimal(500));
    }

    public QuestionWeb[] vote(String votingId, String votes) {
        return null;
    }
}
