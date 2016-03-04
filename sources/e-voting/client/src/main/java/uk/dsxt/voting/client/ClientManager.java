package uk.dsxt.voting.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import uk.dsxt.voting.client.datamodel.QuestionWeb;
import uk.dsxt.voting.client.datamodel.VotingChoice;
import uk.dsxt.voting.client.datamodel.VotingInfoWeb;
import uk.dsxt.voting.client.datamodel.VotingWeb;
import uk.dsxt.voting.common.domain.dataModel.Client;
import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.nodes.AssetsHolder;
import uk.dsxt.voting.common.iso20022.jaxb.MeetingInstruction;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Log4j2
@Value
public class ClientManager {
    ConcurrentMap<String, Participant> participantsById = new ConcurrentHashMap<>();
    ConcurrentMap<String, Client> clientsById = new ConcurrentHashMap<>();
    ConcurrentMap<String, VotingWeb> votingsById = new ConcurrentHashMap<>();

    private final ObjectMapper mapper = new ObjectMapper();

    MeetingInstruction participantsXml;


    AssetsHolder assetsHolder;

    public ClientManager(AssetsHolder assetsHolder, MeetingInstruction participantsXml) {
        this.assetsHolder = assetsHolder;
        this.participantsXml = participantsXml;
    }

    public VotingWeb[] getVotings() {
        return votingsById.values().toArray(new VotingWeb[0]);
    }

    public VotingInfoWeb getVoting(String votingId) {
        return new VotingInfoWeb(votingsById.get(votingId) == null ? null : votingsById.get(votingId).getQuestions(), new BigDecimal(500));
    }

    public boolean vote(String votingId, String votingChoice) {
        try {
            VotingChoice choice = mapper.readValue(votingChoice, VotingChoice.class);
            return true;
        } catch (Exception e) {
            log.error("vote failed. Couldn't deserialize votingChoice. votingId=() votingChoice={}", votingId, votingChoice, e.getMessage());
            return false;
        }
    }

    public QuestionWeb[] votingResults(String votingId) {
        return new QuestionWeb[0];
    }

    public long getTime(String votingId) {
        return 0;
    }
}
