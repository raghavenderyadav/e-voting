package uk.dsxt.voting.client.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Value;

import java.util.Map;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VotingChoice {
    @JsonUnwrapped
    Map<String, QuestionChoice> questionChoices;

    @JsonCreator
    public VotingChoice(Map<String, QuestionChoice> questionChoices) {
        this.questionChoices = questionChoices;
    }
}
