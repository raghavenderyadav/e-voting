package uk.dsxt.voting.client.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import uk.dsxt.voting.common.domain.dataModel.Voting;

import java.util.Arrays;
import java.util.stream.Collectors;

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

    public VotingWeb(Voting v) {
        this.id = v.getId();
        this.name = v.getName();
        this.beginTimestamp = v.getBeginTimestamp();
        this.endTimestamp = v.getEndTimestamp();
        this.questions = Arrays.stream(v.getQuestions()).map(QuestionWeb::new).collect(Collectors.toList()).toArray(new QuestionWeb[v.getQuestions().length]);
    }
}
