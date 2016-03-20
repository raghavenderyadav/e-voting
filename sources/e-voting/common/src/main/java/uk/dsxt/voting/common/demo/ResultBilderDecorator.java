package uk.dsxt.voting.common.demo;

import uk.dsxt.voting.common.domain.dataModel.*;
import uk.dsxt.voting.common.domain.nodes.NetworkClient;
import uk.dsxt.voting.common.domain.nodes.NetworkMessagesSender;

public class ResultBilderDecorator implements NetworkClient {

    private final NetworkClient networkClient;

    private final ResultsBuilder resultsBuilder;

    private final String holderId;

    public ResultBilderDecorator(ResultsBuilder resultsBuilder, NetworkClient networkClient, String holderId) {
        this.resultsBuilder = resultsBuilder;
        this.networkClient = networkClient;
        this.holderId = holderId;
    }

    @Override
    public void addVoting(Voting voting) {
        networkClient.addVoting(voting);
    }

    @Override
    public void addVotingTotalResult(VoteResult result) {
        networkClient.addVotingTotalResult(result);
        resultsBuilder.addResult(holderId, result.toString());
    }

    @Override
    public void addVoteStatus(VoteStatus status) {
        networkClient.addVoteStatus(status);
    }

    @Override
    public void addVote(VoteResult result, String messageId) {
        networkClient.addVote(result, messageId);
    }

    @Override
    public void setNetworkMessagesSender(NetworkMessagesSender networkMessagesSender) {
        networkClient.setNetworkMessagesSender(new NetworkMessagesSender() {
            @Override
            public String addVoting(Voting voting) {
                return networkMessagesSender.addVoting(voting);
            }

            @Override
            public String addVotingTotalResult(VoteResult result) {
                return networkMessagesSender.addVotingTotalResult(result);
            }

            @Override
            public String addVoteStatus(VoteStatus status) {
                return networkMessagesSender.addVoteStatus(status);
            }

            @Override
            public String addVote(VoteResult result, String ownerSignature, String nodeSignature) {
                resultsBuilder.addVote(result.toString());
                return networkMessagesSender.addVote(result, ownerSignature, nodeSignature);
            }
        });
    }
}
