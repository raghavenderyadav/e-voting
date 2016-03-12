package uk.dsxt.voting.client.datamodel;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestResult<T> {
    @JsonProperty("result")
    private final T result;
    @JsonProperty("error")
    private final String error;

    public T getResult() {
        return result;
    }

    @JsonCreator
    public RequestResult(@JsonProperty("return") T result, @JsonProperty("error") String error) {
        this.result = result;
        this.error = error;
    }

    public RequestResult(APIException ex) {
        this.result = null;
        this.error = ex.name();
    }
}
