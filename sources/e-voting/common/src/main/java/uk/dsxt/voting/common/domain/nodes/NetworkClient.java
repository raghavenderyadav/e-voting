package uk.dsxt.voting.common.domain.nodes;

public interface NetworkClient extends NetworkMessagesReceiver {
    
    void setNetworkMessagesSender(NetworkMessagesSender networkMessagesSender);
}
