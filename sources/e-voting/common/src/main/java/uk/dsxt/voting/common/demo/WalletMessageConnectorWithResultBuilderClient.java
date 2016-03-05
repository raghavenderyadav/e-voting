package uk.dsxt.voting.common.demo;

import uk.dsxt.voting.common.domain.dataModel.Participant;
import uk.dsxt.voting.common.domain.dataModel.VoteResult;
import uk.dsxt.voting.common.domain.dataModel.VoteResultStatus;
import uk.dsxt.voting.common.domain.dataModel.Voting;
import uk.dsxt.voting.common.domain.nodes.ClientNode;
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
            public void addVote(VoteResult result) {
                messageReceiver.addVote(result);
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
        }, serializer, cryptoHelper, participantsById, privateKey, holderId, masterId);

    }

    @Override
    public void addVote(VoteResult result, String signature, String receiverId) {
        connector.addVote(result, signature, receiverId);
        if (result.getStatus() == VoteResultStatus.OK && result.getHolderId().indexOf(ClientNode.PATH_SEPARATOR) > 0)
            resultsBuilder.addVote(result.toString());
    }

    @Override
    public void addVoting(Voting voting) {
        connector.addVoting(voting);
    }

    @Override
    public void addVotingTotalResult(VoteResult result) {
        connector.addVotingTotalResult(result);
    }

    public void handleNewMessage(MessageContent messageContent, String messageId) {
        connector.handleNewMessage(messageContent, messageId);
    }
}
