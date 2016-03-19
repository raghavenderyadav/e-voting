package uk.dsxt.voting.common.demo;

import uk.dsxt.voting.common.domain.dataModel.*;
import uk.dsxt.voting.common.domain.nodes.NetworkMessagesReceiver;
import uk.dsxt.voting.common.domain.nodes.NetworkMessagesSender;
import uk.dsxt.voting.common.messaging.MessageContent;
import uk.dsxt.voting.common.messaging.MessagesSerializer;
import uk.dsxt.voting.common.networking.WalletManager;
import uk.dsxt.voting.common.networking.WalletMessageConnector;
import uk.dsxt.voting.common.utils.crypto.CryptoHelper;

import java.security.PrivateKey;
import java.util.Map;

public class WalletMessageConnectorWithResultBuilderClient implements NetworkMessagesSender {

    private final WalletMessageConnector connector;

    private final ResultsBuilder resultsBuilder;

    public WalletMessageConnectorWithResultBuilderClient(ResultsBuilder resultsBuilder,
                                                         WalletManager walletManager, NetworkMessagesReceiver messageReceiver, MessagesSerializer serializer,
                                                         CryptoHelper cryptoHelper, Map<String, Participant> participantsById,
                                                         PrivateKey privateKey, String holderId, String masterId) {
        this.resultsBuilder = resultsBuilder;
        connector = new WalletMessageConnector(walletManager, new NetworkMessagesReceiver() {
            @Override
            public void addVote(VoteResult result, String messageId) {
                messageReceiver.addVote(result, messageId);
            }

            @Override
            public void addVoting(Voting voting) {
                messageReceiver.addVoting(voting);
            }

            @Override
            public void addVotingTotalResult(VoteResult result) {
                messageReceiver.addVotingTotalResult(result);
                resultsBuilder.addResult(holderId, result.toString());
            }

            @Override
            public void addVoteStatus(VoteStatus status) {
                messageReceiver.addVoteStatus(status);
            }
        }, serializer, cryptoHelper, participantsById, privateKey, holderId, masterId);

    }

    @Override
    public String addVote(VoteResult result, String signature, String nodeSignature) {
        resultsBuilder.addVote(result.toString());
        return connector.addVote(result, signature, nodeSignature);
    }

    @Override
    public String addVoting(Voting voting) {
        return connector.addVoting(voting);
    }

    @Override
    public String addVotingTotalResult(VoteResult result) {
        return connector.addVotingTotalResult(result);
    }

    @Override
    public String addVoteStatus(VoteStatus status) {
        return connector.addVoteStatus(status);
    }

    public void handleNewMessage(MessageContent messageContent, String messageId) {
        connector.handleNewMessage(messageContent, messageId);
    }
}
