package uk.dsxt.voting.client.datamodel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Map;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionChoice {
    @JsonUnwrapped
    Map<String, BigDecimal> answerChoices;

    @JsonCreator
    public QuestionChoice(Map<String, BigDecimal> answerChoices) {
        this.answerChoices = answerChoices;
    }
}
