package uk.dsxt.voting.client.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class VotingWeb {
    String id;
    String name;
    long beginTimestamp;
    long endTimestamp;
    QuestionWeb[] questions;

    @JsonCreator
    public VotingWeb(@JsonProperty("id") String id, @JsonProperty("name") String name, @JsonProperty("beginTimestamp") long beginTimestamp, @JsonProperty("endTimestamp") long endTimestamp,
                  @JsonProperty("questions") QuestionWeb[] questions) {
        this.id = id;
        this.name = name;
        this.beginTimestamp = beginTimestamp;
        this.endTimestamp = endTimestamp;
        this.questions = questions;
    }
}
